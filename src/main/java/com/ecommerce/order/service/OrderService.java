package com.ecommerce.order.service;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.common.exception.ErrorDetail;
import com.ecommerce.order.dto.OrderCreateRequest;
import com.ecommerce.order.dto.OrderCreateResponse;
import com.ecommerce.order.dto.OrderDetailResponse;
import com.ecommerce.order.dto.OrderPaymentCancelResponse;
import com.ecommerce.order.dto.OrderPaymentSuccessResponse;
import com.ecommerce.order.dto.OrderProductCouponRequest;
import com.ecommerce.order.dto.OrderSummaryResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderCancelReason;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.exception.OrderNotPaymentPendingException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.exception.OrderPaymentExpiredException;
import com.ecommerce.order.exception.OrderValidationException;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.entity.Stock;
import com.ecommerce.product.repository.StockRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final long PAYMENT_PENDING_MINUTES = 10;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final StockRepository stockRepository;
    private final EntityManager entityManager;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            StockRepository stockRepository,
            EntityManager entityManager
    ) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.stockRepository = stockRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public OrderCreateResponse createOrder(Long customerId, OrderCreateRequest request) {
        Long validCustomerId = requirePositiveCustomerId(customerId);
        OrderCreateRequest validRequest = Objects.requireNonNull(request, "주문 생성 요청은 필수입니다.");
        List<Long> cartItemIds = requireUniqueCartItemIds(validRequest.cartItemIds());

        Cart cart = cartRepository.findForOrderByCustomerId(validCustomerId).orElse(null);
        Map<Long, CartItem> cartItemsById = getCartItemsById(cart);
        List<CartItem> selectedItems = findSelectedItems(cartItemIds, cartItemsById);
        List<ErrorDetail> details = new ArrayList<>(validateSelectedItems(cartItemIds, selectedItems));
        details.addAll(validateUnsupportedCoupons(validRequest));

        if (!details.isEmpty()) {
            throw new OrderValidationException(details);
        }

        Map<Long, Stock> lockedStocksByProductId = lockStocks(selectedItems);
        List<ErrorDetail> stockDetails = validateOrderableItems(selectedItems, lockedStocksByProductId);
        if (!stockDetails.isEmpty()) {
            throw new OrderValidationException(stockDetails);
        }

        List<OrderItem> orderItems = selectedItems.stream()
                .map(cartItem -> reserveStockAndCreateOrderItem(cartItem, lockedStocksByProductId))
                .toList();
        Order order = Order.create(validCustomerId, LocalDateTime.now().plusMinutes(PAYMENT_PENDING_MINUTES), orderItems);
        Order savedOrder = orderRepository.save(order);

        return OrderCreateResponse.from(savedOrder);
    }

    public List<OrderSummaryResponse> getOrders(Long customerId) {
        return orderRepository.findAllByCustomerIdOrderByIdDesc(requirePositiveCustomerId(customerId))
                .stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    public OrderDetailResponse getOrder(Long customerId, Long orderId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, requirePositiveCustomerId(customerId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return OrderDetailResponse.from(order);
    }

    @Transactional(noRollbackFor = OrderPaymentExpiredException.class)
    public OrderPaymentSuccessResponse completePayment(Long customerId, Long orderId) {
        Order order = findPaymentTargetOrder(customerId, orderId);
        LocalDateTime now = LocalDateTime.now();
        if (order.isPaymentExpired(now)) {
            cancelOrderAndRestoreStock(order, OrderCancelReason.PAYMENT_EXPIRED, now);
            throw new OrderPaymentExpiredException(order.getId());
        }

        order.completePayment(now);
        removeOrderedCartItems(order);

        return OrderPaymentSuccessResponse.from(order);
    }

    @Transactional
    public OrderPaymentCancelResponse cancelPayment(Long customerId, Long orderId) {
        Order order = findPaymentTargetOrder(customerId, orderId);
        cancelOrderAndRestoreStock(order, OrderCancelReason.PAYMENT_CANCELED, LocalDateTime.now());

        return OrderPaymentCancelResponse.from(order);
    }

    private Order findPaymentTargetOrder(Long customerId, Long orderId) {
        Long validCustomerId = requirePositiveCustomerId(customerId);
        Long validOrderId = requirePositiveOrderId(orderId);
        Order order = orderRepository.findByIdAndCustomerIdForUpdate(validOrderId, validCustomerId)
                .orElseThrow(() -> new OrderNotFoundException(validOrderId));
        if (!order.isPaymentPending()) {
            throw new OrderNotPaymentPendingException(validOrderId);
        }

        return order;
    }

    private void cancelOrderAndRestoreStock(Order order, OrderCancelReason cancelReason, LocalDateTime canceledAt) {
        order.cancelPayment(cancelReason, canceledAt);
        restoreReservedStocks(order);
    }

    private void restoreReservedStocks(Order order) {
        Map<Long, Long> quantitiesByProductId = order.getItems()
                .stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getId(),
                        Collectors.summingLong(OrderItem::getQuantity)
                ));
        if (quantitiesByProductId.isEmpty()) {
            return;
        }

        Map<Long, Stock> lockedStocksByProductId = stockRepository.findAllByProductIdInForUpdate(
                        quantitiesByProductId.keySet()
                )
                .stream()
                .collect(Collectors.toMap(stock -> stock.getProduct().getId(), Function.identity()));
        lockedStocksByProductId.values()
                .forEach(stock -> entityManager.refresh(stock, LockModeType.PESSIMISTIC_WRITE));

        quantitiesByProductId.forEach((productId, quantity) -> {
            Stock stock = lockedStocksByProductId.get(productId);
            if (stock == null) {
                throw new IllegalStateException("주문 상품 재고를 찾을 수 없습니다. productId=" + productId);
            }
            stock.increase(quantity);
        });
    }

    private void removeOrderedCartItems(Order order) {
        Cart cart = cartRepository.findForOrderByCustomerId(order.getCustomerId()).orElse(null);
        if (cart == null) {
            return;
        }

        Map<Long, Long> orderedQuantitiesByCartItemId = order.getItems()
                .stream()
                .filter(orderItem -> orderItem.getSourceCartItemId() != null)
                .collect(Collectors.groupingBy(
                        OrderItem::getSourceCartItemId,
                        Collectors.summingLong(OrderItem::getQuantity)
                ));
        orderedQuantitiesByCartItemId.forEach((cartItemId, orderedQuantity) -> cart.findItemById(cartItemId)
                .ifPresent(cartItem -> removeOrderedQuantity(cart, cartItem, orderedQuantity)));
    }

    private void removeOrderedQuantity(Cart cart, CartItem cartItem, long orderedQuantity) {
        if (cartItem.getQuantity() <= orderedQuantity) {
            cart.removeItem(cartItem);
            return;
        }

        cartItem.changeQuantity(cartItem.getQuantity() - orderedQuantity);
    }

    private List<CartItem> findSelectedItems(List<Long> cartItemIds, Map<Long, CartItem> cartItemsById) {
        return cartItemIds.stream()
                .map(cartItemsById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ErrorDetail> validateSelectedItems(List<Long> cartItemIds, List<CartItem> selectedItems) {
        Set<Long> selectedIds = selectedItems.stream()
                .map(CartItem::getId)
                .collect(Collectors.toSet());

        return cartItemIds.stream()
                .filter(cartItemId -> !selectedIds.contains(cartItemId))
                .map(cartItemId -> ErrorDetail.cartItem(cartItemId, "CART_ITEM_NOT_FOUND"))
                .toList();
    }

    private List<ErrorDetail> validateUnsupportedCoupons(OrderCreateRequest request) {
        List<ErrorDetail> details = new ArrayList<>();
        if (request.orderCouponId() != null) {
            details.add(ErrorDetail.coupon(request.orderCouponId(), "COUPON_NOT_AVAILABLE"));
        }

        List<OrderProductCouponRequest> productCoupons = request.productCoupons();
        if (productCoupons == null) {
            return details;
        }

        productCoupons.stream()
                .filter(Objects::nonNull)
                .map(OrderProductCouponRequest::couponId)
                .filter(Objects::nonNull)
                .map(couponId -> ErrorDetail.coupon(couponId, "COUPON_NOT_AVAILABLE"))
                .forEach(details::add);

        return details;
    }

    private Map<Long, Stock> lockStocks(List<CartItem> selectedItems) {
        Set<Long> productIds = selectedItems.stream()
                .map(CartItem::getProduct)
                .map(Product::getId)
                .collect(Collectors.toSet());

        Map<Long, Stock> lockedStocksByProductId = stockRepository.findAllByProductIdInForUpdate(productIds)
                .stream()
                .collect(Collectors.toMap(stock -> stock.getProduct().getId(), Function.identity()));
        lockedStocksByProductId.values()
                .forEach(stock -> entityManager.refresh(stock, LockModeType.PESSIMISTIC_WRITE));

        return lockedStocksByProductId;
    }

    private List<ErrorDetail> validateOrderableItems(List<CartItem> selectedItems, Map<Long, Stock> lockedStocksByProductId) {
        List<ErrorDetail> details = new ArrayList<>();
        for (CartItem cartItem : selectedItems) {
            Product product = cartItem.getProduct();
            Stock stock = lockedStocksByProductId.get(product.getId());
            if (product.getStatus() != ProductStatus.ON_SALE) {
                details.add(ErrorDetail.product(product.getId(), "PRODUCT_NOT_ON_SALE"));
            }
            if (stock == null || !stock.hasEnough(cartItem.getQuantity())) {
                long currentStock = stock == null ? 0 : stock.getQuantity();
                details.add(ErrorDetail.productStock(product.getId(), "OUT_OF_STOCK", currentStock));
            }
        }

        return details;
    }

    private OrderItem reserveStockAndCreateOrderItem(CartItem cartItem, Map<Long, Stock> lockedStocksByProductId) {
        Product product = cartItem.getProduct();
        lockedStocksByProductId.get(product.getId()).decrease(cartItem.getQuantity());

        return OrderItem.create(product, cartItem.getQuantity(), cartItem.getId());
    }

    private Map<Long, CartItem> getCartItemsById(Cart cart) {
        if (cart == null) {
            return Map.of();
        }

        return cart.getItems()
                .stream()
                .collect(Collectors.toMap(CartItem::getId, Function.identity()));
    }

    private static Long requirePositiveCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("고객 ID는 1 이상이어야 합니다.");
        }

        return customerId;
    }

    private static Long requirePositiveOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("주문 ID는 1 이상이어야 합니다.");
        }

        return orderId;
    }

    private static List<Long> requireUniqueCartItemIds(Collection<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new IllegalArgumentException("주문할 장바구니 상품은 1개 이상이어야 합니다.");
        }

        List<Long> normalizedIds = new ArrayList<>();
        Set<Long> duplicatedIds = new HashSet<>();
        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (Long cartItemId : cartItemIds) {
            if (cartItemId == null || cartItemId <= 0) {
                throw new IllegalArgumentException("장바구니 상품 ID는 1 이상이어야 합니다.");
            }
            if (!uniqueIds.add(cartItemId)) {
                duplicatedIds.add(cartItemId);
            }
            normalizedIds.add(cartItemId);
        }

        if (!duplicatedIds.isEmpty()) {
            throw new IllegalArgumentException("주문할 장바구니 상품 ID는 중복될 수 없습니다.");
        }

        return normalizedIds;
    }
}

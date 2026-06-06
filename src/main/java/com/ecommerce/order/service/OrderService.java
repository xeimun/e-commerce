package com.ecommerce.order.service;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.common.exception.ErrorDetail;
import com.ecommerce.coupon.entity.IssuedCoupon;
import com.ecommerce.coupon.repository.IssuedCouponRepository;
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
import com.ecommerce.order.entity.OrderStatus;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private final IssuedCouponRepository issuedCouponRepository;
    private final EntityManager entityManager;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            StockRepository stockRepository,
            IssuedCouponRepository issuedCouponRepository,
            EntityManager entityManager
    ) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.stockRepository = stockRepository;
        this.issuedCouponRepository = issuedCouponRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public OrderCreateResponse createOrder(Long customerId, OrderCreateRequest request) {
        Long validCustomerId = requirePositiveCustomerId(customerId);
        OrderCreateRequest validRequest = Objects.requireNonNull(request, "주문 생성 요청은 필수입니다.");
        List<Long> cartItemIds = requireUniqueCartItemIds(validRequest.cartItemIds());
        LocalDateTime now = LocalDateTime.now();

        Cart cart = cartRepository.findForOrderByCustomerId(validCustomerId).orElse(null);
        Map<Long, CartItem> cartItemsById = getCartItemsById(cart);
        List<CartItem> selectedItems = findSelectedItems(cartItemIds, cartItemsById);
        List<ErrorDetail> details = new ArrayList<>(validateSelectedItems(cartItemIds, selectedItems));

        if (!details.isEmpty()) {
            throw new OrderValidationException(details);
        }

        CouponApplication couponApplication = validateCouponApplication(validCustomerId, validRequest, selectedItems, now);
        Map<Long, Stock> lockedStocksByProductId = lockStocks(selectedItems);
        List<ErrorDetail> stockDetails = validateOrderableItems(selectedItems, lockedStocksByProductId);
        if (!stockDetails.isEmpty()) {
            throw new OrderValidationException(stockDetails);
        }

        Map<Long, BigDecimal> instantDiscountsByCartItemId = calculateInstantDiscounts(selectedItems, now);
        Map<Long, BigDecimal> productCouponDiscountsByCartItemId =
                calculateProductCouponDiscounts(selectedItems, couponApplication, now);
        List<OrderItem> orderItems = selectedItems.stream()
                .map(cartItem -> reserveStockAndCreateOrderItem(
                        cartItem,
                        lockedStocksByProductId,
                        instantDiscountsByCartItemId.getOrDefault(cartItem.getId(), BigDecimal.ZERO),
                        productCouponDiscountsByCartItemId.getOrDefault(cartItem.getId(), BigDecimal.ZERO)
                ))
                .toList();
        BigDecimal orderCouponDiscountAmount = calculateOrderCouponDiscount(couponApplication.orderCoupon(), orderItems);
        Order order = Order.create(
                validCustomerId,
                now.plusMinutes(PAYMENT_PENDING_MINUTES),
                orderItems,
                orderCouponDiscountAmount
        );
        Order savedOrder = orderRepository.save(order);
        reserveCoupons(couponApplication, savedOrder, now);

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
        useReservedCoupons(order, now);
        removeOrderedCartItems(order);

        return OrderPaymentSuccessResponse.from(order);
    }

    @Transactional
    public OrderPaymentCancelResponse cancelPayment(Long customerId, Long orderId) {
        Order order = findPaymentTargetOrder(customerId, orderId);
        cancelOrderAndRestoreStock(order, OrderCancelReason.PAYMENT_CANCELED, LocalDateTime.now());

        return OrderPaymentCancelResponse.from(order);
    }

    @Transactional
    public int expirePaymentPendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderRepository.findExpiredOrdersForUpdate(OrderStatus.PAYMENT_PENDING, now);
        cancelOrdersAndRestoreStock(expiredOrders, OrderCancelReason.PAYMENT_EXPIRED, now);

        return expiredOrders.size();
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
        restoreReservedStocks(List.of(order));
        releaseReservedCoupons(order);
    }

    private void cancelOrdersAndRestoreStock(List<Order> orders, OrderCancelReason cancelReason, LocalDateTime canceledAt) {
        if (orders.isEmpty()) {
            return;
        }

        orders.forEach(order -> order.cancelPayment(cancelReason, canceledAt));
        restoreReservedStocks(orders);
        orders.forEach(this::releaseReservedCoupons);
    }

    private void restoreReservedStocks(List<Order> orders) {
        Map<Long, Long> quantitiesByProductId = orders.stream()
                .flatMap(order -> order.getItems().stream())
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

    private CouponApplication validateCouponApplication(
            Long customerId,
            OrderCreateRequest request,
            List<CartItem> selectedItems,
            LocalDateTime now
    ) {
        Map<Long, CartItem> selectedItemsById = selectedItems.stream()
                .collect(Collectors.toMap(CartItem::getId, Function.identity()));
        CouponRequest couponRequest = normalizeCouponRequest(request, selectedItemsById);
        if (couponRequest.isEmpty()) {
            return CouponApplication.empty();
        }

        Map<Long, IssuedCoupon> issuedCouponsById = issuedCouponRepository.findAllByIdInForUpdate(couponRequest.couponIds())
                .stream()
                .collect(Collectors.toMap(IssuedCoupon::getId, Function.identity()));
        List<ErrorDetail> details = new ArrayList<>();

        IssuedCoupon orderCoupon = null;
        if (couponRequest.orderCouponId() != null) {
            orderCoupon = issuedCouponsById.get(couponRequest.orderCouponId());
            details.addAll(validateIssuedCoupon(
                    couponRequest.orderCouponId(),
                    orderCoupon,
                    customerId,
                    now
            ));
            if (details.isEmpty() && !orderCoupon.getCoupon().isOrderCoupon()) {
                details.add(ErrorDetail.coupon(couponRequest.orderCouponId(), "COUPON_TARGET_MISMATCH"));
            }
        }

        Map<Long, IssuedCoupon> productCouponsByCartItemId = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> entry : couponRequest.productCouponIdsByCartItemId().entrySet()) {
            Long cartItemId = entry.getKey();
            Long couponId = entry.getValue();
            IssuedCoupon issuedCoupon = issuedCouponsById.get(couponId);
            List<ErrorDetail> couponDetails = validateIssuedCoupon(
                    couponId,
                    issuedCoupon,
                    customerId,
                    now
            );
            details.addAll(couponDetails);
            if (!couponDetails.isEmpty()) {
                continue;
            }

            CartItem cartItem = selectedItemsById.get(cartItemId);
            if (!issuedCoupon.getCoupon().isProductCouponFor(cartItem.getProduct())) {
                details.add(ErrorDetail.coupon(couponId, "COUPON_TARGET_MISMATCH"));
                continue;
            }
            productCouponsByCartItemId.put(cartItemId, issuedCoupon);
        }

        if (!details.isEmpty()) {
            throw new OrderValidationException(details);
        }

        return new CouponApplication(orderCoupon, productCouponsByCartItemId);
    }

    private CouponRequest normalizeCouponRequest(OrderCreateRequest request, Map<Long, CartItem> selectedItemsById) {
        Long orderCouponId = normalizeOptionalPositiveId(request.orderCouponId(), "전체 상품 쿠폰 ID");
        Map<Long, Long> productCouponIdsByCartItemId = new LinkedHashMap<>();
        List<ErrorDetail> details = new ArrayList<>();

        for (OrderProductCouponRequest productCoupon : normalizeProductCoupons(request.productCoupons())) {
            Long cartItemId = requirePositiveId(productCoupon.cartItemId(), "쿠폰 적용 장바구니 상품 ID");
            Long couponId = requirePositiveId(productCoupon.couponId(), "상품 쿠폰 ID");
            if (!selectedItemsById.containsKey(cartItemId)) {
                details.add(ErrorDetail.coupon(couponId, "COUPON_TARGET_MISMATCH"));
                continue;
            }
            Long previousCouponId = productCouponIdsByCartItemId.putIfAbsent(cartItemId, couponId);
            if (previousCouponId != null) {
                details.add(ErrorDetail.coupon(couponId, "COUPON_NOT_AVAILABLE"));
            }
        }
        details.addAll(validateDuplicateCouponIds(orderCouponId, productCouponIdsByCartItemId.values()));

        if (!details.isEmpty()) {
            throw new OrderValidationException(details);
        }

        return new CouponRequest(orderCouponId, productCouponIdsByCartItemId);
    }

    private List<OrderProductCouponRequest> normalizeProductCoupons(List<OrderProductCouponRequest> productCoupons) {
        if (productCoupons == null || productCoupons.isEmpty()) {
            return List.of();
        }

        return productCoupons.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ErrorDetail> validateDuplicateCouponIds(Long orderCouponId, Collection<Long> productCouponIds) {
        List<ErrorDetail> details = new ArrayList<>();
        Set<Long> uniqueCouponIds = new HashSet<>();
        if (orderCouponId != null) {
            uniqueCouponIds.add(orderCouponId);
        }
        productCouponIds.stream()
                .filter(couponId -> !uniqueCouponIds.add(couponId))
                .map(couponId -> ErrorDetail.coupon(couponId, "COUPON_NOT_AVAILABLE"))
                .forEach(details::add);

        return details;
    }

    private List<ErrorDetail> validateIssuedCoupon(
            Long requestedCouponId,
            IssuedCoupon issuedCoupon,
            Long customerId,
            LocalDateTime now
    ) {
        if (issuedCoupon == null || !issuedCoupon.isOwnedBy(customerId)) {
            return List.of(ErrorDetail.coupon(requestedCouponId, "COUPON_NOT_OWNED"));
        }
        if (!issuedCoupon.isAvailableAt(now)) {
            return List.of(ErrorDetail.coupon(requestedCouponId, issuedCoupon.unavailableReason(now)));
        }

        return List.of();
    }

    private Map<Long, BigDecimal> calculateProductCouponDiscounts(
            List<CartItem> selectedItems,
            CouponApplication couponApplication,
            LocalDateTime now
    ) {
        Map<Long, BigDecimal> productCouponDiscountsByCartItemId = new LinkedHashMap<>();
        Map<Long, CartItem> selectedItemsById = selectedItems.stream()
                .collect(Collectors.toMap(CartItem::getId, Function.identity()));
        couponApplication.productCouponsByCartItemId().forEach((cartItemId, issuedCoupon) -> {
            CartItem cartItem = selectedItemsById.get(cartItemId);
            Product product = cartItem.getProduct();
            BigDecimal productAmountAfterInstantDiscount = product.getPrice()
                    .subtract(product.getInstantDiscountAmount(now))
                    .max(BigDecimal.ZERO);
            BigDecimal discountAmount = min(
                    issuedCoupon.getCoupon().getDiscountAmount(),
                    productAmountAfterInstantDiscount
            );
            productCouponDiscountsByCartItemId.put(cartItemId, discountAmount);
        });

        return productCouponDiscountsByCartItemId;
    }

    private Map<Long, BigDecimal> calculateInstantDiscounts(List<CartItem> selectedItems, LocalDateTime now) {
        return selectedItems.stream()
                .collect(Collectors.toMap(
                        CartItem::getId,
                        cartItem -> cartItem.getProduct().calculateInstantDiscountAmount(now, cartItem.getQuantity())
                ));
    }

    private BigDecimal calculateOrderCouponDiscount(IssuedCoupon orderCoupon, List<OrderItem> orderItems) {
        if (orderCoupon == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal paymentAmountBeforeOrderCoupon = orderItems.stream()
                .map(OrderItem::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return min(orderCoupon.getCoupon().getDiscountAmount(), paymentAmountBeforeOrderCoupon);
    }

    private void reserveCoupons(CouponApplication couponApplication, Order order, LocalDateTime reservedAt) {
        couponApplication.issuedCoupons()
                .forEach(issuedCoupon -> issuedCoupon.reserve(order, reservedAt));
    }

    private void useReservedCoupons(Order order, LocalDateTime usedAt) {
        issuedCouponRepository.findAllByOrderIdForUpdate(order.getId())
                .forEach(issuedCoupon -> issuedCoupon.use(usedAt));
    }

    private void releaseReservedCoupons(Order order) {
        issuedCouponRepository.findAllByOrderIdForUpdate(order.getId())
                .forEach(IssuedCoupon::releaseReservation);
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

    private OrderItem reserveStockAndCreateOrderItem(
            CartItem cartItem,
            Map<Long, Stock> lockedStocksByProductId,
            BigDecimal instantDiscountAmount,
            BigDecimal productCouponDiscountAmount
    ) {
        Product product = cartItem.getProduct();
        lockedStocksByProductId.get(product.getId()).decrease(cartItem.getQuantity());

        return OrderItem.create(
                product,
                cartItem.getQuantity(),
                cartItem.getId(),
                instantDiscountAmount,
                productCouponDiscountAmount
        );
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

    private static Long normalizeOptionalPositiveId(Long id, String fieldName) {
        if (id == null) {
            return null;
        }

        return requirePositiveId(id, fieldName);
    }

    private static Long requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + "는 1 이상이어야 합니다.");
        }

        return id;
    }

    private static BigDecimal min(BigDecimal first, BigDecimal second) {
        if (first.compareTo(second) <= 0) {
            return first;
        }

        return second;
    }

    private record CouponRequest(Long orderCouponId, Map<Long, Long> productCouponIdsByCartItemId) {

        boolean isEmpty() {
            return orderCouponId == null && productCouponIdsByCartItemId.isEmpty();
        }

        Set<Long> couponIds() {
            Set<Long> couponIds = new LinkedHashSet<>();
            if (orderCouponId != null) {
                couponIds.add(orderCouponId);
            }
            couponIds.addAll(productCouponIdsByCartItemId.values());

            return couponIds;
        }
    }

    private record CouponApplication(
            IssuedCoupon orderCoupon,
            Map<Long, IssuedCoupon> productCouponsByCartItemId
    ) {

        static CouponApplication empty() {
            return new CouponApplication(null, Map.of());
        }

        List<IssuedCoupon> issuedCoupons() {
            List<IssuedCoupon> issuedCoupons = new ArrayList<>();
            if (orderCoupon != null) {
                issuedCoupons.add(orderCoupon);
            }
            issuedCoupons.addAll(productCouponsByCartItemId.values());

            return issuedCoupons;
        }
    }
}

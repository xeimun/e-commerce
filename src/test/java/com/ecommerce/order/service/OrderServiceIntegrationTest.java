package com.ecommerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.coupon.entity.Coupon;
import com.ecommerce.coupon.entity.IssuedCoupon;
import com.ecommerce.coupon.entity.IssuedCouponStatus;
import com.ecommerce.coupon.repository.CouponRepository;
import com.ecommerce.coupon.repository.IssuedCouponRepository;
import com.ecommerce.order.dto.OrderCreateRequest;
import com.ecommerce.order.dto.OrderCreateResponse;
import com.ecommerce.order.dto.OrderProductCouponRequest;
import com.ecommerce.order.dto.OrderPaymentCancelResponse;
import com.ecommerce.order.dto.OrderPaymentSuccessResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderCancelReason;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentStatus;
import com.ecommerce.order.exception.OrderPaymentExpiredException;
import com.ecommerce.order.exception.OrderValidationException;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.entity.ContentType;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.entity.Stock;
import com.ecommerce.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @BeforeEach
    void setUp() {
        issuedCouponRepository.deleteAll();
        couponRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void createOrderPersistsOrderAndDecreasesStock() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 5));
        Cart cart = Cart.create(7L);
        CartItem cartItem = cart.addItem(product, 2);
        cartRepository.save(cart);

        OrderCreateResponse response = orderService.createOrder(
                7L,
                new OrderCreateRequest(List.of(cartItem.getId()), null, List.of())
        );

        Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
        Order foundOrder = orderRepository.findByIdAndCustomerId(response.orderId(), 7L).orElseThrow();

        assertThat(response.status()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(response.totalProductAmount()).isEqualByComparingTo("70000.00");
        assertThat(response.finalPaymentAmount()).isEqualByComparingTo("70000.00");
        assertThat(foundProduct.getStock().getQuantity()).isEqualTo(3);
        assertThat(foundOrder.getItems()).hasSize(1);
        assertThat(foundOrder.getItems().get(0).getProductName()).isEqualTo("달빛 상점 한정판 아트북");
        assertThat(foundOrder.getItems().get(0).getSourceCartItemId()).isEqualTo(cartItem.getId());
    }

    @Test
    void createOrderPersistsInstantDiscountSnapshot() {
        Product product = productFixture(ProductStatus.ON_SALE, 5);
        product.applyInstantDiscount(
                "드롭 오픈 할인",
                new BigDecimal("3000.00"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );
        Product savedProduct = productRepository.save(product);
        Cart cart = Cart.create(7L);
        CartItem cartItem = cart.addItem(savedProduct, 2);
        cartRepository.save(cart);

        OrderCreateResponse response = orderService.createOrder(
                7L,
                new OrderCreateRequest(List.of(cartItem.getId()), null, List.of())
        );

        Order foundOrder = orderRepository.findByIdAndCustomerId(response.orderId(), 7L).orElseThrow();
        assertThat(response.totalProductAmount()).isEqualByComparingTo("70000.00");
        assertThat(response.totalInstantDiscountAmount()).isEqualByComparingTo("6000.00");
        assertThat(response.finalPaymentAmount()).isEqualByComparingTo("64000.00");
        assertThat(foundOrder.getTotalInstantDiscountAmount()).isEqualByComparingTo("6000.00");
        assertThat(foundOrder.getItems().get(0).getInstantDiscountAmount()).isEqualByComparingTo("6000.00");
        assertThat(foundOrder.getItems().get(0).getFinalAmount()).isEqualByComparingTo("64000.00");
    }

    @Test
    void createOrderPersistsCouponReservationsAndCouponDiscounts() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 5));
        Coupon orderCoupon = couponRepository.save(Coupon.createOrderCoupon(
                "전체 상품 할인 쿠폰",
                new BigDecimal("10000.00"),
                LocalDateTime.now().plusDays(1)
        ));
        Coupon productCoupon = couponRepository.save(Coupon.createProductCoupon(
                "특정 상품 할인 쿠폰",
                new BigDecimal("5000.00"),
                product,
                LocalDateTime.now().plusDays(1)
        ));
        IssuedCoupon issuedOrderCoupon = issuedCouponRepository.save(
                IssuedCoupon.issue(orderCoupon, 7L, LocalDateTime.now().minusDays(1))
        );
        IssuedCoupon issuedProductCoupon = issuedCouponRepository.save(
                IssuedCoupon.issue(productCoupon, 7L, LocalDateTime.now().minusDays(1))
        );
        Cart cart = Cart.create(7L);
        CartItem cartItem = cart.addItem(product, 2);
        cartRepository.save(cart);

        OrderCreateResponse response = orderService.createOrder(
                7L,
                new OrderCreateRequest(
                        List.of(cartItem.getId()),
                        issuedOrderCoupon.getId(),
                        List.of(new OrderProductCouponRequest(cartItem.getId(), issuedProductCoupon.getId()))
                )
        );

        Order foundOrder = orderRepository.findByIdAndCustomerId(response.orderId(), 7L).orElseThrow();
        IssuedCoupon foundOrderCoupon = issuedCouponRepository.findById(issuedOrderCoupon.getId()).orElseThrow();
        IssuedCoupon foundProductCoupon = issuedCouponRepository.findById(issuedProductCoupon.getId()).orElseThrow();
        assertThat(response.totalProductAmount()).isEqualByComparingTo("70000.00");
        assertThat(response.totalCouponDiscountAmount()).isEqualByComparingTo("15000.00");
        assertThat(response.finalPaymentAmount()).isEqualByComparingTo("55000.00");
        assertThat(foundOrder.getTotalCouponDiscountAmount()).isEqualByComparingTo("15000.00");
        assertThat(foundOrder.getFinalPaymentAmount()).isEqualByComparingTo("55000.00");
        assertThat(foundOrder.getItems().get(0).getProductCouponDiscountAmount()).isEqualByComparingTo("5000.00");
        assertThat(foundOrderCoupon.getStatus()).isEqualTo(IssuedCouponStatus.RESERVED);
        assertThat(foundOrderCoupon.getOrder().getId()).isEqualTo(response.orderId());
        assertThat(foundProductCoupon.getStatus()).isEqualTo(IssuedCouponStatus.RESERVED);
        assertThat(foundProductCoupon.getOrder().getId()).isEqualTo(response.orderId());
    }

    @Test
    void createOrderRejectsSecondPaymentPendingOrderWithFirstOrderCoupon() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 5));
        Coupon firstCoupon = couponRepository.save(firstOrderCouponFixture("첫 주문 전체 상품 3000원 할인"));
        Coupon secondCoupon = couponRepository.save(firstOrderCouponFixture("첫 주문 전체 상품 5000원 할인"));
        IssuedCoupon firstIssuedCoupon = issuedCouponRepository.save(
                IssuedCoupon.issue(firstCoupon, 7L, LocalDateTime.now().minusDays(1))
        );
        IssuedCoupon secondIssuedCoupon = issuedCouponRepository.save(
                IssuedCoupon.issue(secondCoupon, 7L, LocalDateTime.now().minusDays(1))
        );
        Cart cart = Cart.create(7L);
        CartItem cartItem = cart.addItem(product, 1);
        cartRepository.save(cart);
        orderService.createOrder(
                7L,
                new OrderCreateRequest(List.of(cartItem.getId()), firstIssuedCoupon.getId(), List.of())
        );

        assertThatThrownBy(() -> orderService.createOrder(
                7L,
                new OrderCreateRequest(List.of(cartItem.getId()), secondIssuedCoupon.getId(), List.of())
        ))
                .isInstanceOfSatisfying(OrderValidationException.class, exception -> assertThat(exception.getDetails())
                        .extracting("reason")
                        .containsOnly("FIRST_ORDER_COUPON_NOT_AVAILABLE"));
    }

    @Test
    void createOrderFailureDoesNotPersistOrderOrDecreaseStock() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 1));
        Cart cart = Cart.create(7L);
        CartItem cartItem = cart.addItem(product, 2);
        cartRepository.save(cart);

        assertThatThrownBy(() -> orderService.createOrder(
                7L,
                new OrderCreateRequest(List.of(cartItem.getId()), null, List.of())
        ))
                .isInstanceOf(OrderValidationException.class);

        Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(foundProduct.getStock().getQuantity()).isEqualTo(1);
        assertThat(orderRepository.findAllByCustomerIdOrderByIdDesc(7L)).isEmpty();
    }

    @Test
    void completePaymentPersistsCompletedOrderAndRemovesOrderedCartItems() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 5));
        Cart cart = Cart.create(7L);
        CartItem cartItem = cart.addItem(product, 2);
        cartRepository.save(cart);
        OrderCreateResponse orderResponse = orderService.createOrder(
                7L,
                new OrderCreateRequest(List.of(cartItem.getId()), null, List.of())
        );

        OrderPaymentSuccessResponse response = orderService.completePayment(7L, orderResponse.orderId());

        Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
        Order foundOrder = orderRepository.findByIdAndCustomerId(response.orderId(), 7L).orElseThrow();
        Cart foundCart = cartRepository.findByCustomerId(7L).orElseThrow();
        assertThat(response.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.paidAt()).isNotNull();
        assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(foundOrder.getPaidAt()).isNotNull();
        assertThat(foundProduct.getStock().getQuantity()).isEqualTo(3);
        assertThat(foundCart.getItems()).isEmpty();
    }

    @Test
    void completePaymentPersistsUsedCoupon() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 5));
        IssuedCoupon issuedCoupon = issuedCouponRepository.save(IssuedCoupon.issue(
                couponRepository.save(Coupon.createOrderCoupon(
                        "전체 상품 할인 쿠폰",
                        new BigDecimal("10000.00"),
                        LocalDateTime.now().plusDays(1)
                )),
                7L,
                LocalDateTime.now().minusDays(1)
        ));
        Cart cart = Cart.create(7L);
        CartItem cartItem = cart.addItem(product, 2);
        cartRepository.save(cart);
        OrderCreateResponse orderResponse = orderService.createOrder(
                7L,
                new OrderCreateRequest(List.of(cartItem.getId()), issuedCoupon.getId(), List.of())
        );

        orderService.completePayment(7L, orderResponse.orderId());

        IssuedCoupon foundIssuedCoupon = issuedCouponRepository.findById(issuedCoupon.getId()).orElseThrow();
        assertThat(foundIssuedCoupon.getStatus()).isEqualTo(IssuedCouponStatus.USED);
        assertThat(foundIssuedCoupon.getUsedAt()).isNotNull();
    }

    @Test
    void completePaymentRejectsReservedFirstOrderCouponWhenCustomerAlreadyHasCompletedOrder() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 5));
        Order completedOrder = Order.create(
                7L,
                LocalDateTime.now().plusMinutes(5),
                List.of(OrderItem.create(product, 1))
        );
        completedOrder.completePayment(LocalDateTime.now().minusMinutes(1));
        orderRepository.save(completedOrder);
        Order pendingOrder = orderRepository.save(Order.create(
                7L,
                LocalDateTime.now().plusMinutes(5),
                List.of(OrderItem.create(product, 1))
        ));
        IssuedCoupon issuedCoupon = issuedCouponRepository.save(IssuedCoupon.issue(
                couponRepository.save(firstOrderCouponFixture("첫 주문 전체 상품 3000원 할인")),
                7L,
                LocalDateTime.now().minusDays(1)
        ));
        issuedCoupon.reserve(pendingOrder, LocalDateTime.now().minusMinutes(1));
        issuedCouponRepository.saveAndFlush(issuedCoupon);

        assertThatThrownBy(() -> orderService.completePayment(7L, pendingOrder.getId()))
                .isInstanceOfSatisfying(OrderValidationException.class, exception -> assertThat(exception.getDetails())
                        .extracting("reason")
                        .containsOnly("FIRST_ORDER_COUPON_NOT_AVAILABLE"));

        Order foundPendingOrder = orderRepository.findByIdAndCustomerId(pendingOrder.getId(), 7L).orElseThrow();
        IssuedCoupon foundIssuedCoupon = issuedCouponRepository.findById(issuedCoupon.getId()).orElseThrow();
        assertThat(foundPendingOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(foundIssuedCoupon.getStatus()).isEqualTo(IssuedCouponStatus.RESERVED);
    }

    @Test
    void completePaymentKeepsQuantityAddedToSourceCartItemAfterOrderCreation() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 5));
        Cart cart = Cart.create(7L);
        CartItem cartItem = cart.addItem(product, 2);
        cartRepository.save(cart);
        Long sourceCartItemId = cartItem.getId();
        OrderCreateResponse orderResponse = orderService.createOrder(
                7L,
                new OrderCreateRequest(List.of(sourceCartItemId), null, List.of())
        );

        Cart cartAfterOrder = cartRepository.findByCustomerId(7L).orElseThrow();
        Product cartProduct = cartAfterOrder.findItemById(sourceCartItemId).orElseThrow().getProduct();
        cartAfterOrder.addItem(cartProduct, 1);
        cartRepository.saveAndFlush(cartAfterOrder);

        orderService.completePayment(7L, orderResponse.orderId());

        Cart foundCart = cartRepository.findByCustomerId(7L).orElseThrow();
        assertThat(foundCart.getItems()).hasSize(1);
        assertThat(foundCart.getItems().get(0).getId()).isEqualTo(sourceCartItemId);
        assertThat(foundCart.getItems().get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    void completePaymentDoesNotRemoveReaddedCartItemWithSameProduct() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 5));
        Cart cart = Cart.create(7L);
        CartItem originalCartItem = cart.addItem(product, 2);
        cartRepository.save(cart);
        Long originalCartItemId = originalCartItem.getId();
        OrderCreateResponse orderResponse = orderService.createOrder(
                7L,
                new OrderCreateRequest(List.of(originalCartItemId), null, List.of())
        );

        Cart cartAfterOrder = cartRepository.findByCustomerId(7L).orElseThrow();
        CartItem itemToDelete = cartAfterOrder.findItemById(originalCartItemId).orElseThrow();
        cartAfterOrder.removeItem(itemToDelete);
        cartRepository.saveAndFlush(cartAfterOrder);

        Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
        Cart cartAfterDelete = cartRepository.findByCustomerId(7L).orElseThrow();
        cartAfterDelete.addItem(foundProduct, 1);
        Cart savedCart = cartRepository.saveAndFlush(cartAfterDelete);
        Long readdedCartItemId = savedCart.findItemByProductId(product.getId()).orElseThrow().getId();

        orderService.completePayment(7L, orderResponse.orderId());

        Cart foundCart = cartRepository.findByCustomerId(7L).orElseThrow();
        assertThat(foundCart.getItems()).hasSize(1);
        assertThat(foundCart.getItems().get(0).getId()).isEqualTo(readdedCartItemId);
        assertThat(foundCart.getItems().get(0).getProduct().getId()).isEqualTo(product.getId());
    }

    @Test
    void cancelPaymentPersistsCanceledOrderAndRestoresReservedStock() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 5));
        Cart cart = Cart.create(7L);
        CartItem cartItem = cart.addItem(product, 2);
        cartRepository.save(cart);
        OrderCreateResponse orderResponse = orderService.createOrder(
                7L,
                new OrderCreateRequest(List.of(cartItem.getId()), null, List.of())
        );

        OrderPaymentCancelResponse response = orderService.cancelPayment(7L, orderResponse.orderId());

        Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
        Order foundOrder = orderRepository.findByIdAndCustomerId(response.orderId(), 7L).orElseThrow();
        Cart foundCart = cartRepository.findByCustomerId(7L).orElseThrow();
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(response.cancelReason()).isEqualTo(OrderCancelReason.PAYMENT_CANCELED);
        assertThat(foundOrder.getCancelReason()).isEqualTo(OrderCancelReason.PAYMENT_CANCELED);
        assertThat(foundOrder.getPaymentCanceledAt()).isNotNull();
        assertThat(foundProduct.getStock().getQuantity()).isEqualTo(5);
        assertThat(foundCart.getItems()).hasSize(1);
    }

    @Test
    void cancelPaymentPersistsAvailableCoupon() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 5));
        IssuedCoupon issuedCoupon = issuedCouponRepository.save(IssuedCoupon.issue(
                couponRepository.save(Coupon.createOrderCoupon(
                        "전체 상품 할인 쿠폰",
                        new BigDecimal("10000.00"),
                        LocalDateTime.now().plusDays(1)
                )),
                7L,
                LocalDateTime.now().minusDays(1)
        ));
        Cart cart = Cart.create(7L);
        CartItem cartItem = cart.addItem(product, 2);
        cartRepository.save(cart);
        OrderCreateResponse orderResponse = orderService.createOrder(
                7L,
                new OrderCreateRequest(List.of(cartItem.getId()), issuedCoupon.getId(), List.of())
        );

        orderService.cancelPayment(7L, orderResponse.orderId());

        IssuedCoupon foundIssuedCoupon = issuedCouponRepository.findById(issuedCoupon.getId()).orElseThrow();
        assertThat(foundIssuedCoupon.getStatus()).isEqualTo(IssuedCouponStatus.AVAILABLE);
        assertThat(foundIssuedCoupon.getOrder()).isNull();
        assertThat(foundIssuedCoupon.getReservedAt()).isNull();
        assertThat(foundIssuedCoupon.getUsedAt()).isNull();
    }

    @Test
    void completePaymentForExpiredOrderPersistsCanceledOrderAndRestoresReservedStock() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 3));
        Cart cart = Cart.create(7L);
        cart.addItem(product, 2);
        cartRepository.save(cart);
        Order order = Order.create(
                7L,
                LocalDateTime.now().minusMinutes(1),
                List.of(OrderItem.create(product, 2))
        );
        Order savedOrder = orderRepository.save(order);

        assertThatThrownBy(() -> orderService.completePayment(7L, savedOrder.getId()))
                .isInstanceOf(OrderPaymentExpiredException.class);

        Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
        Order foundOrder = orderRepository.findByIdAndCustomerId(savedOrder.getId(), 7L).orElseThrow();
        Cart foundCart = cartRepository.findByCustomerId(7L).orElseThrow();
        assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(foundOrder.getCancelReason()).isEqualTo(OrderCancelReason.PAYMENT_EXPIRED);
        assertThat(foundOrder.getPaymentCanceledAt()).isNotNull();
        assertThat(foundProduct.getStock().getQuantity()).isEqualTo(5);
        assertThat(foundCart.getItems()).hasSize(1);
    }

    @Test
    void expirePaymentPendingOrdersPersistsCanceledOrdersAndRestoresReservedStock() {
        Product product = productRepository.save(productFixture(ProductStatus.ON_SALE, 3));
        Order order = Order.create(
                7L,
                LocalDateTime.now().minusMinutes(1),
                List.of(OrderItem.create(product, 2))
        );
        Order savedOrder = orderRepository.save(order);

        int expiredCount = orderService.expirePaymentPendingOrders();

        Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
        Order foundOrder = orderRepository.findByIdAndCustomerId(savedOrder.getId(), 7L).orElseThrow();
        assertThat(expiredCount).isEqualTo(1);
        assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(foundOrder.getCancelReason()).isEqualTo(OrderCancelReason.PAYMENT_EXPIRED);
        assertThat(foundOrder.getPaymentCanceledAt()).isNotNull();
        assertThat(foundProduct.getStock().getQuantity()).isEqualTo(5);
    }

    private Product productFixture(ProductStatus status, long stockQuantity) {
        Product product = Product.create(
                "달빛 상점 한정판 아트북",
                new BigDecimal("35000.00"),
                status,
                "달빛 상점",
                ContentType.WEBTOON,
                "ARTBOOK",
                "웹툰 달빛 상점의 시즌 1 한정판 아트북"
        );
        Stock stock = Stock.create(product, stockQuantity);
        product.registerStock(stock);

        return product;
    }

    private Coupon firstOrderCouponFixture(String name) {
        return Coupon.createFirstOrderCoupon(
                name,
                new BigDecimal("3000.00"),
                LocalDateTime.now().plusDays(1)
        );
    }
}

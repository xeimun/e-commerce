package com.ecommerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.common.exception.ErrorDetail;
import com.ecommerce.coupon.entity.Coupon;
import com.ecommerce.coupon.entity.IssuedCoupon;
import com.ecommerce.coupon.entity.IssuedCouponStatus;
import com.ecommerce.coupon.repository.IssuedCouponRepository;
import com.ecommerce.order.dto.OrderCreateRequest;
import com.ecommerce.order.dto.OrderCreateResponse;
import com.ecommerce.order.dto.OrderPaymentCancelResponse;
import com.ecommerce.order.dto.OrderPaymentSuccessResponse;
import com.ecommerce.order.dto.OrderProductCouponRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderCancelReason;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentStatus;
import com.ecommerce.order.exception.OrderNotPaymentPendingException;
import com.ecommerce.order.exception.OrderPaymentExpiredException;
import com.ecommerce.order.exception.OrderValidationException;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.entity.ContentType;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.entity.Stock;
import com.ecommerce.product.repository.StockRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private EntityManager entityManager;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                cartRepository,
                stockRepository,
                issuedCouponRepository,
                entityManager
        );
    }

    @Test
    void createOrderCreatesPaymentPendingOrderAndReservesStock() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 5);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 1L);
            return order;
        });

        OrderCreateResponse response = orderService.createOrder(7L, new OrderCreateRequest(List.of(10L), null, List.of()));

        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(response.totalProductAmount()).isEqualByComparingTo("70000.00");
        assertThat(response.finalPaymentAmount()).isEqualByComparingTo("70000.00");
        assertThat(product.getStock().getQuantity()).isEqualTo(3);
        verify(entityManager).refresh(product.getStock(), LockModeType.PESSIMISTIC_WRITE);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrderThrowsExceptionWhenCartItemDoesNotExist() {
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(7L, new OrderCreateRequest(List.of(10L), null, List.of())))
                .isInstanceOfSatisfying(OrderValidationException.class, exception -> {
                    assertThat(exception.getDetails()).hasSize(1);
                    assertThat(exception.getDetails().get(0).reason()).isEqualTo("CART_ITEM_NOT_FOUND");
                });
        verify(stockRepository, never()).findAllByProductIdInForUpdate(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderThrowsExceptionWhenProductIsNotOnSale() {
        Product product = productFixture(1L, ProductStatus.STOPPED, 5);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));

        assertThatThrownBy(() -> orderService.createOrder(7L, new OrderCreateRequest(List.of(10L), null, List.of())))
                .isInstanceOfSatisfying(OrderValidationException.class, exception -> {
                    assertThat(exception.getDetails()).hasSize(1);
                    assertThat(exception.getDetails().get(0).reason()).isEqualTo("PRODUCT_NOT_ON_SALE");
                });
        assertThat(product.getStock().getQuantity()).isEqualTo(5);
        verify(entityManager).refresh(product.getStock(), LockModeType.PESSIMISTIC_WRITE);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderThrowsExceptionWhenStockIsNotEnough() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 1);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));

        assertThatThrownBy(() -> orderService.createOrder(7L, new OrderCreateRequest(List.of(10L), null, List.of())))
                .isInstanceOfSatisfying(OrderValidationException.class, exception -> {
                    assertThat(exception.getDetails()).hasSize(1);
                    assertThat(exception.getDetails().get(0).reason()).isEqualTo("OUT_OF_STOCK");
                    assertThat(exception.getDetails().get(0).currentStock()).isEqualTo(1);
                });
        assertThat(product.getStock().getQuantity()).isEqualTo(1);
        verify(entityManager).refresh(product.getStock(), LockModeType.PESSIMISTIC_WRITE);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderThrowsExceptionWhenRequestedCouponIsNotOwned() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 5);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(issuedCouponRepository.findAllByIdInForUpdate(any())).thenReturn(List.of());

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(10L),
                100L,
                List.of(new OrderProductCouponRequest(10L, 200L))
        );

        assertThatThrownBy(() -> orderService.createOrder(7L, request))
                .isInstanceOfSatisfying(OrderValidationException.class, exception -> {
                    assertThat(exception.getDetails()).hasSize(2);
                    assertThat(exception.getDetails())
                            .extracting(ErrorDetail::reason)
                            .containsOnly("COUPON_NOT_OWNED");
                });
        verify(stockRepository, never()).findAllByProductIdInForUpdate(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderReservesCouponsAndAppliesCouponDiscounts() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 5);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        IssuedCoupon orderCoupon = issuedOrderCouponFixture(100L, 7L, new BigDecimal("10000.00"));
        IssuedCoupon productCoupon = issuedProductCouponFixture(200L, 7L, product, new BigDecimal("5000.00"));
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(issuedCouponRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(orderCoupon, productCoupon));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 1L);
            return order;
        });

        OrderCreateResponse response = orderService.createOrder(
                7L,
                new OrderCreateRequest(
                        List.of(10L),
                        100L,
                        List.of(new OrderProductCouponRequest(10L, 200L))
                )
        );

        assertThat(response.totalProductAmount()).isEqualByComparingTo("70000.00");
        assertThat(response.totalCouponDiscountAmount()).isEqualByComparingTo("15000.00");
        assertThat(response.finalPaymentAmount()).isEqualByComparingTo("55000.00");
        assertThat(product.getStock().getQuantity()).isEqualTo(3);
        assertThat(orderCoupon.getStatus()).isEqualTo(IssuedCouponStatus.RESERVED);
        assertThat(orderCoupon.getOrder().getId()).isEqualTo(1L);
        assertThat(orderCoupon.getReservedAt()).isNotNull();
        assertThat(productCoupon.getStatus()).isEqualTo(IssuedCouponStatus.RESERVED);
        assertThat(productCoupon.getOrder().getId()).isEqualTo(1L);
        assertThat(productCoupon.getReservedAt()).isNotNull();
    }

    @Test
    void createOrderAppliesInstantDiscountByQuantity() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 5);
        product.applyInstantDiscount(
                "드롭 오픈 할인",
                new BigDecimal("3000.00"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 1L);
            return order;
        });

        OrderCreateResponse response = orderService.createOrder(7L, new OrderCreateRequest(List.of(10L), null, List.of()));

        assertThat(response.totalProductAmount()).isEqualByComparingTo("70000.00");
        assertThat(response.totalInstantDiscountAmount()).isEqualByComparingTo("6000.00");
        assertThat(response.finalPaymentAmount()).isEqualByComparingTo("64000.00");
    }

    @Test
    void createOrderAppliesProductCouponAfterInstantDiscountForOneUnit() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 5);
        product.applyInstantDiscount(
                "드롭 오픈 할인",
                new BigDecimal("10000.00"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        IssuedCoupon productCoupon = issuedProductCouponFixture(200L, 7L, product, new BigDecimal("50000.00"));
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(issuedCouponRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(productCoupon));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 1L);
            return order;
        });

        OrderCreateResponse response = orderService.createOrder(
                7L,
                new OrderCreateRequest(
                        List.of(10L),
                        null,
                        List.of(new OrderProductCouponRequest(10L, 200L))
                )
        );

        assertThat(response.totalProductAmount()).isEqualByComparingTo("70000.00");
        assertThat(response.totalInstantDiscountAmount()).isEqualByComparingTo("20000.00");
        assertThat(response.totalCouponDiscountAmount()).isEqualByComparingTo("25000.00");
        assertThat(response.finalPaymentAmount()).isEqualByComparingTo("25000.00");
    }

    @Test
    void createOrderThrowsExceptionWhenCouponIsExpired() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 5);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        IssuedCoupon issuedCoupon = issuedOrderCouponFixture(
                100L,
                7L,
                new BigDecimal("10000.00"),
                LocalDateTime.now().minusMinutes(1)
        );
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(issuedCouponRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(issuedCoupon));

        assertThatThrownBy(() -> orderService.createOrder(
                7L,
                new OrderCreateRequest(List.of(10L), 100L, List.of())
        ))
                .isInstanceOfSatisfying(OrderValidationException.class, exception -> assertThat(exception.getDetails())
                        .extracting(ErrorDetail::reason)
                        .containsOnly("COUPON_EXPIRED"));
        verify(stockRepository, never()).findAllByProductIdInForUpdate(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderThrowsExceptionWhenProductCouponTargetDoesNotMatch() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 5);
        Product anotherProduct = productFixture(2L, ProductStatus.ON_SALE, 5);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        IssuedCoupon issuedCoupon = issuedProductCouponFixture(
                200L,
                7L,
                anotherProduct,
                new BigDecimal("5000.00")
        );
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(issuedCouponRepository.findAllByIdInForUpdate(any())).thenReturn(List.of(issuedCoupon));

        assertThatThrownBy(() -> orderService.createOrder(
                7L,
                new OrderCreateRequest(
                        List.of(10L),
                        null,
                        List.of(new OrderProductCouponRequest(10L, 200L))
                )
        ))
                .isInstanceOfSatisfying(OrderValidationException.class, exception -> assertThat(exception.getDetails())
                        .extracting(ErrorDetail::reason)
                        .containsOnly("COUPON_TARGET_MISMATCH"));
        verify(stockRepository, never()).findAllByProductIdInForUpdate(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void completePaymentCompletesPendingOrderAndRemovesOrderedCartItems() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 3);
        Order order = orderFixture(1L, 7L, product, 2, 10L, LocalDateTime.now().plusMinutes(5));
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        when(orderRepository.findByIdAndCustomerIdForUpdate(1L, 7L)).thenReturn(Optional.of(order));
        when(issuedCouponRepository.findAllByOrderIdForUpdate(1L)).thenReturn(List.of());
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.of(cart));

        OrderPaymentSuccessResponse response = orderService.completePayment(7L, 1L);

        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.paidAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(cart.getItems()).isEmpty();
        assertThat(product.getStock().getQuantity()).isEqualTo(3);
        verify(stockRepository, never()).findAllByProductIdInForUpdate(any());
    }

    @Test
    void completePaymentKeepsQuantityAddedToSourceCartItemAfterOrderCreation() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 3);
        Order order = orderFixture(1L, 7L, product, 2, 10L, LocalDateTime.now().plusMinutes(5));
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 3);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        when(orderRepository.findByIdAndCustomerIdForUpdate(1L, 7L)).thenReturn(Optional.of(order));
        when(issuedCouponRepository.findAllByOrderIdForUpdate(1L)).thenReturn(List.of());
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.of(cart));

        orderService.completePayment(7L, 1L);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getId()).isEqualTo(10L);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    void completePaymentDoesNotRemoveReaddedCartItemWithSameProduct() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 3);
        Order order = orderFixture(1L, 7L, product, 2, 10L, LocalDateTime.now().plusMinutes(5));
        Cart cart = cartFixture(7L);
        CartItem readdedCartItem = cart.addItem(product, 1);
        ReflectionTestUtils.setField(readdedCartItem, "id", 11L);
        when(orderRepository.findByIdAndCustomerIdForUpdate(1L, 7L)).thenReturn(Optional.of(order));
        when(issuedCouponRepository.findAllByOrderIdForUpdate(1L)).thenReturn(List.of());
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.of(cart));

        orderService.completePayment(7L, 1L);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getId()).isEqualTo(11L);
        assertThat(cart.getItems().get(0).getProduct().getId()).isEqualTo(1L);
    }

    @Test
    void completePaymentUsesReservedCoupons() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 3);
        Order order = orderFixture(1L, 7L, product, 2, 10L, LocalDateTime.now().plusMinutes(5));
        IssuedCoupon issuedCoupon = issuedOrderCouponFixture(100L, 7L, new BigDecimal("10000.00"));
        issuedCoupon.reserve(order, LocalDateTime.now().minusMinutes(1));
        when(orderRepository.findByIdAndCustomerIdForUpdate(1L, 7L)).thenReturn(Optional.of(order));
        when(issuedCouponRepository.findAllByOrderIdForUpdate(1L)).thenReturn(List.of(issuedCoupon));
        when(cartRepository.findForOrderByCustomerId(7L)).thenReturn(Optional.empty());

        orderService.completePayment(7L, 1L);

        assertThat(issuedCoupon.getStatus()).isEqualTo(IssuedCouponStatus.USED);
        assertThat(issuedCoupon.getUsedAt()).isNotNull();
    }

    @Test
    void cancelPaymentCancelsPendingOrderAndRestoresReservedStock() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 3);
        Order order = orderFixture(1L, 7L, product, 2, null, LocalDateTime.now().plusMinutes(5));
        when(orderRepository.findByIdAndCustomerIdForUpdate(1L, 7L)).thenReturn(Optional.of(order));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));
        when(issuedCouponRepository.findAllByOrderIdForUpdate(1L)).thenReturn(List.of());

        OrderPaymentCancelResponse response = orderService.cancelPayment(7L, 1L);

        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(response.cancelReason()).isEqualTo(OrderCancelReason.PAYMENT_CANCELED);
        assertThat(order.getCancelReason()).isEqualTo(OrderCancelReason.PAYMENT_CANCELED);
        assertThat(order.getPaymentCanceledAt()).isNotNull();
        assertThat(product.getStock().getQuantity()).isEqualTo(5);
        verify(entityManager).refresh(product.getStock(), LockModeType.PESSIMISTIC_WRITE);
        verify(cartRepository, never()).findForOrderByCustomerId(7L);
    }

    @Test
    void cancelPaymentReleasesReservedCoupons() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 3);
        Order order = orderFixture(1L, 7L, product, 2, null, LocalDateTime.now().plusMinutes(5));
        IssuedCoupon issuedCoupon = issuedOrderCouponFixture(100L, 7L, new BigDecimal("10000.00"));
        issuedCoupon.reserve(order, LocalDateTime.now().minusMinutes(1));
        when(orderRepository.findByIdAndCustomerIdForUpdate(1L, 7L)).thenReturn(Optional.of(order));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));
        when(issuedCouponRepository.findAllByOrderIdForUpdate(1L)).thenReturn(List.of(issuedCoupon));

        orderService.cancelPayment(7L, 1L);

        assertThat(issuedCoupon.getStatus()).isEqualTo(IssuedCouponStatus.AVAILABLE);
        assertThat(issuedCoupon.getOrder()).isNull();
        assertThat(issuedCoupon.getReservedAt()).isNull();
    }

    @Test
    void completePaymentThrowsExceptionAndCancelsOrderWhenPaymentIsExpired() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 3);
        Order order = orderFixture(1L, 7L, product, 2, null, LocalDateTime.now().minusMinutes(1));
        when(orderRepository.findByIdAndCustomerIdForUpdate(1L, 7L)).thenReturn(Optional.of(order));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));
        when(issuedCouponRepository.findAllByOrderIdForUpdate(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.completePayment(7L, 1L))
                .isInstanceOf(OrderPaymentExpiredException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCancelReason()).isEqualTo(OrderCancelReason.PAYMENT_EXPIRED);
        assertThat(order.getPaymentCanceledAt()).isNotNull();
        assertThat(product.getStock().getQuantity()).isEqualTo(5);
        verify(entityManager).refresh(product.getStock(), LockModeType.PESSIMISTIC_WRITE);
        verify(cartRepository, never()).findForOrderByCustomerId(7L);
    }

    @Test
    void expirePaymentPendingOrdersCancelsExpiredOrdersAndRestoresReservedStock() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 3);
        Order order = orderFixture(1L, 7L, product, 2, null, LocalDateTime.now().minusMinutes(1));
        when(orderRepository.findExpiredOrdersForUpdate(eq(OrderStatus.PAYMENT_PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(order));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));
        when(issuedCouponRepository.findAllByOrderIdForUpdate(1L)).thenReturn(List.of());

        int expiredCount = orderService.expirePaymentPendingOrders();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCancelReason()).isEqualTo(OrderCancelReason.PAYMENT_EXPIRED);
        assertThat(order.getPaymentCanceledAt()).isNotNull();
        assertThat(product.getStock().getQuantity()).isEqualTo(5);
        verify(entityManager).refresh(product.getStock(), LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void expirePaymentPendingOrdersReleasesReservedCoupons() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 3);
        Order order = orderFixture(1L, 7L, product, 2, null, LocalDateTime.now().minusMinutes(1));
        IssuedCoupon issuedCoupon = issuedOrderCouponFixture(100L, 7L, new BigDecimal("10000.00"));
        issuedCoupon.reserve(order, LocalDateTime.now().minusMinutes(2));
        when(orderRepository.findExpiredOrdersForUpdate(eq(OrderStatus.PAYMENT_PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(order));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));
        when(issuedCouponRepository.findAllByOrderIdForUpdate(1L)).thenReturn(List.of(issuedCoupon));

        orderService.expirePaymentPendingOrders();

        assertThat(issuedCoupon.getStatus()).isEqualTo(IssuedCouponStatus.AVAILABLE);
        assertThat(issuedCoupon.getOrder()).isNull();
        assertThat(issuedCoupon.getReservedAt()).isNull();
    }

    @Test
    void expirePaymentPendingOrdersDoesNothingWhenExpiredOrderDoesNotExist() {
        when(orderRepository.findExpiredOrdersForUpdate(eq(OrderStatus.PAYMENT_PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of());

        int expiredCount = orderService.expirePaymentPendingOrders();

        assertThat(expiredCount).isZero();
        verify(stockRepository, never()).findAllByProductIdInForUpdate(any());
    }

    @Test
    void completePaymentThrowsExceptionWhenOrderIsNotPaymentPending() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 3);
        Order order = orderFixture(1L, 7L, product, 2, null, LocalDateTime.now().plusMinutes(5));
        order.completePayment(LocalDateTime.now());
        when(orderRepository.findByIdAndCustomerIdForUpdate(1L, 7L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.completePayment(7L, 1L))
                .isInstanceOf(OrderNotPaymentPendingException.class);

        verify(stockRepository, never()).findAllByProductIdInForUpdate(any());
        verify(cartRepository, never()).findForOrderByCustomerId(7L);
    }

    private Cart cartFixture(Long customerId) {
        Cart cart = Cart.create(customerId);
        ReflectionTestUtils.setField(cart, "id", 1L);

        return cart;
    }

    private Order orderFixture(
            Long orderId,
            Long customerId,
            Product product,
            long quantity,
            Long sourceCartItemId,
            LocalDateTime expiresAt
    ) {
        Order order = Order.create(customerId, expiresAt, List.of(OrderItem.create(product, quantity, sourceCartItemId)));
        ReflectionTestUtils.setField(order, "id", orderId);

        return order;
    }

    private Product productFixture(Long productId, ProductStatus status, long stockQuantity) {
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
        ReflectionTestUtils.setField(product, "id", productId);

        return product;
    }

    private IssuedCoupon issuedOrderCouponFixture(Long issuedCouponId, Long customerId, BigDecimal discountAmount) {
        return issuedOrderCouponFixture(issuedCouponId, customerId, discountAmount, LocalDateTime.now().plusDays(1));
    }

    private IssuedCoupon issuedOrderCouponFixture(
            Long issuedCouponId,
            Long customerId,
            BigDecimal discountAmount,
            LocalDateTime expiresAt
    ) {
        Coupon coupon = Coupon.createOrderCoupon(
                "전체 상품 할인 쿠폰",
                discountAmount,
                expiresAt
        );
        ReflectionTestUtils.setField(coupon, "id", issuedCouponId + 1000);
        IssuedCoupon issuedCoupon = IssuedCoupon.issue(coupon, customerId, LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(issuedCoupon, "id", issuedCouponId);

        return issuedCoupon;
    }

    private IssuedCoupon issuedProductCouponFixture(
            Long issuedCouponId,
            Long customerId,
            Product product,
            BigDecimal discountAmount
    ) {
        Coupon coupon = Coupon.createProductCoupon(
                "특정 상품 할인 쿠폰",
                discountAmount,
                product,
                LocalDateTime.now().plusDays(1)
        );
        ReflectionTestUtils.setField(coupon, "id", issuedCouponId + 1000);
        IssuedCoupon issuedCoupon = IssuedCoupon.issue(coupon, customerId, LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(issuedCoupon, "id", issuedCouponId);

        return issuedCoupon;
    }
}

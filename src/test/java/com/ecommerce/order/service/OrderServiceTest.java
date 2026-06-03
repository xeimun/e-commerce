package com.ecommerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.common.exception.ErrorDetail;
import com.ecommerce.order.dto.OrderCreateRequest;
import com.ecommerce.order.dto.OrderCreateResponse;
import com.ecommerce.order.dto.OrderProductCouponRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.exception.OrderValidationException;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.entity.ContentType;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.entity.Stock;
import com.ecommerce.product.repository.StockRepository;
import java.math.BigDecimal;
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

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, cartRepository, stockRepository);
    }

    @Test
    void createOrderCreatesPaymentPendingOrderAndReservesStock() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 5);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));
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
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrderThrowsExceptionWhenCartItemDoesNotExist() {
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.empty());

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
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));

        assertThatThrownBy(() -> orderService.createOrder(7L, new OrderCreateRequest(List.of(10L), null, List.of())))
                .isInstanceOfSatisfying(OrderValidationException.class, exception -> {
                    assertThat(exception.getDetails()).hasSize(1);
                    assertThat(exception.getDetails().get(0).reason()).isEqualTo("PRODUCT_NOT_ON_SALE");
                });
        assertThat(product.getStock().getQuantity()).isEqualTo(5);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderThrowsExceptionWhenStockIsNotEnough() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 1);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(stockRepository.findAllByProductIdInForUpdate(any())).thenReturn(List.of(product.getStock()));

        assertThatThrownBy(() -> orderService.createOrder(7L, new OrderCreateRequest(List.of(10L), null, List.of())))
                .isInstanceOfSatisfying(OrderValidationException.class, exception -> {
                    assertThat(exception.getDetails()).hasSize(1);
                    assertThat(exception.getDetails().get(0).reason()).isEqualTo("OUT_OF_STOCK");
                    assertThat(exception.getDetails().get(0).currentStock()).isEqualTo(1);
                });
        assertThat(product.getStock().getQuantity()).isEqualTo(1);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderThrowsExceptionWhenCouponIsRequestedBeforeCouponFeature() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 5);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 10L);
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));

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
                            .containsOnly("COUPON_NOT_AVAILABLE");
                });
        verify(stockRepository, never()).findAllByProductIdInForUpdate(any());
        verify(orderRepository, never()).save(any());
    }

    private Cart cartFixture(Long customerId) {
        Cart cart = Cart.create(customerId);
        ReflectionTestUtils.setField(cart, "id", 1L);

        return cart;
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
}

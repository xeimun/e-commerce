package com.ecommerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.order.dto.OrderCreateRequest;
import com.ecommerce.order.dto.OrderCreateResponse;
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

    @BeforeEach
    void setUp() {
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
}

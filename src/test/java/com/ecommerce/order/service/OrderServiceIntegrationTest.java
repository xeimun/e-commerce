package com.ecommerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.order.dto.OrderCreateRequest;
import com.ecommerce.order.dto.OrderCreateResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.exception.OrderValidationException;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.entity.ContentType;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.entity.Stock;
import com.ecommerce.product.repository.ProductRepository;
import java.math.BigDecimal;
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

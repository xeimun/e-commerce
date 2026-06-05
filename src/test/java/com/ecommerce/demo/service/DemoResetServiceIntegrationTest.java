package com.ecommerce.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.coupon.entity.Coupon;
import com.ecommerce.coupon.entity.IssuedCouponStatus;
import com.ecommerce.coupon.repository.CouponRepository;
import com.ecommerce.coupon.repository.IssuedCouponRepository;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.entity.ContentType;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.entity.Stock;
import com.ecommerce.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoResetServiceIntegrationTest {

    @Autowired
    private DemoResetService demoResetService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        issuedCouponRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        couponRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void resetRecreatesDemoProductsCouponsCartAndCustomerCoupon() {
        Product oldProduct = productRepository.save(productFixture());
        couponRepository.save(Coupon.createProductCoupon(
                "이전 쿠폰",
                new BigDecimal("1000.00"),
                oldProduct,
                LocalDateTime.now().plusDays(1)
        ));

        demoResetService.reset();

        assertThat(productRepository.findAllByOrderByIdAsc())
                .hasSize(5)
                .extracting(Product::getName)
                .containsExactly(
                        "달빛 상점 한정판 아트북",
                        "검은별 기록관 포스터 세트",
                        "푸른 사서의 방 아크릴 스탠드",
                        "라스트 오케스트라 OST 패키지",
                        "새벽의 문장 스티커팩"
                );
        assertThat(couponRepository.findAll()).hasSize(3);
        assertThat(cartRepository.findByCustomerId(DemoResetService.DEMO_CUSTOMER_ID))
                .map(Cart::getItems)
                .hasValueSatisfying(items -> assertThat(items).hasSize(2));
        assertThat(issuedCouponRepository.findAllByCustomerIdOrderByIdDesc(DemoResetService.DEMO_CUSTOMER_ID))
                .hasSize(1)
                .allSatisfy(coupon -> assertThat(coupon.getStatus()).isEqualTo(IssuedCouponStatus.AVAILABLE));
        assertThat(orderRepository.findAllByCustomerIdOrderByIdDesc(DemoResetService.DEMO_CUSTOMER_ID)).isEmpty();
    }

    private Product productFixture() {
        Product product = Product.create(
                "기존 상품",
                new BigDecimal("10000.00"),
                ProductStatus.ON_SALE,
                "기존 콘텐츠",
                ContentType.WEBTOON,
                "ARTBOOK",
                "초기화 전 데이터"
        );
        product.registerStock(Stock.create(product, 1));

        return product;
    }
}

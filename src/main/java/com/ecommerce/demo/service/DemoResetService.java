package com.ecommerce.demo.service;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.coupon.entity.Coupon;
import com.ecommerce.coupon.entity.IssuedCoupon;
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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoResetService {

    public static final Long DEMO_CUSTOMER_ID = 1L;

    private final CartRepository cartRepository;
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public DemoResetService(
            CartRepository cartRepository,
            CouponRepository couponRepository,
            IssuedCouponRepository issuedCouponRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository
    ) {
        this.cartRepository = cartRepository;
        this.couponRepository = couponRepository;
        this.issuedCouponRepository = issuedCouponRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void reset() {
        resetDemoCustomerData();

        LocalDateTime now = LocalDateTime.now();
        List<Product> products = productRepository.saveAll(sampleProducts(now));
        List<Coupon> coupons = couponRepository.saveAll(sampleCoupons(products, now));

        cartRepository.save(sampleCart(products));
        issuedCouponRepository.save(IssuedCoupon.issue(coupons.get(0), DEMO_CUSTOMER_ID, now));
    }

    private void resetDemoCustomerData() {
        issuedCouponRepository.deleteAllByCustomerId(DEMO_CUSTOMER_ID);
        orderRepository.deleteAllByCustomerId(DEMO_CUSTOMER_ID);
        cartRepository.deleteByCustomerId(DEMO_CUSTOMER_ID);
        issuedCouponRepository.flush();
        orderRepository.flush();
        cartRepository.flush();
    }

    private List<Product> sampleProducts(LocalDateTime now) {
        Product artbook = product(
                "달빛 상점 한정판 아트북",
                "35000.00",
                "달빛 상점",
                ContentType.WEBTOON,
                "ARTBOOK",
                "웹툰 달빛 상점의 시즌 1 일러스트와 작가 코멘터리를 담은 한정판 아트북",
                24,
                "드롭 오픈 할인",
                "3000.00",
                now.plusDays(30)
        );

        Product poster = product(
                "검은별 기록관 포스터 세트",
                "18000.00",
                "검은별 기록관",
                ContentType.WEB_NOVEL,
                "POSTER",
                "검은별 기록관 주요 장면을 담은 A2 포스터 3종 세트",
                32,
                null,
                null,
                null
        );

        Product stand = product(
                "푸른 사서의 방 아크릴 스탠드",
                "22000.00",
                "푸른 사서의 방",
                ContentType.WEBTOON,
                "ACRYLIC_STAND",
                "주인공과 서가 배경을 함께 세울 수 있는 데스크용 아크릴 스탠드",
                18,
                null,
                null,
                null
        );

        Product ost = product(
                "라스트 오케스트라 OST 패키지",
                "41000.00",
                "라스트 오케스트라",
                ContentType.MUSIC,
                "OST",
                "OST 앨범, 북릿, 한정 넘버링 포토카드를 포함한 패키지",
                12,
                "OST 예약 할인",
                "5000.00",
                now.plusDays(14)
        );

        Product sticker = product(
                "새벽의 문장 스티커팩",
                "9000.00",
                "새벽의 문장",
                ContentType.DRAMA,
                "STICKER",
                "주요 대사와 상징 오브젝트를 담은 다이어리 스티커팩",
                40,
                null,
                null,
                null
        );

        return List.of(artbook, poster, stand, ost, sticker);
    }

    private List<Coupon> sampleCoupons(List<Product> products, LocalDateTime now) {
        return List.of(
                orderCoupon("전체 상품 5000원 할인", "5000.00", now.plusDays(30)),
                productCoupon("달빛 상점 4000원 할인", "4000.00", products.get(0), now.plusDays(21)),
                productCoupon("라스트 오케스트라 3000원 할인", "3000.00", products.get(3), now.plusDays(21))
        );
    }

    private Cart sampleCart(List<Product> products) {
        Cart cart = Cart.create(DEMO_CUSTOMER_ID);
        cart.addItem(products.get(0), 1);
        cart.addItem(products.get(2), 2);

        return cart;
    }

    private Product product(
            String name,
            String price,
            String contentTitle,
            ContentType contentType,
            String category,
            String description,
            long stockQuantity,
            String discountName,
            String discountAmount,
            LocalDateTime discountEndsAt
    ) {
        Product product = productRepository.findFirstByNameOrderByIdAsc(name)
                .orElseGet(() -> Product.create(
                        name,
                        money(price),
                        ProductStatus.ON_SALE,
                        contentTitle,
                        contentType,
                        category,
                        description
                ));

        product.update(name, money(price), contentTitle, contentType, category, description);
        product.changeStatus(ProductStatus.ON_SALE);
        if (product.getStock() == null) {
            product.registerStock(Stock.create(product, stockQuantity));
        } else {
            product.getStock().setQuantity(stockQuantity);
        }
        if (discountName == null) {
            product.deactivateInstantDiscount();
        } else {
            product.applyInstantDiscount(
                    discountName,
                    money(discountAmount),
                    LocalDateTime.now().minusDays(1),
                    discountEndsAt
            );
        }

        return product;
    }

    private Coupon orderCoupon(String name, String discountAmount, LocalDateTime expiresAt) {
        Coupon coupon = couponRepository.findFirstByNameOrderByIdAsc(name)
                .orElseGet(() -> Coupon.createOrderCoupon(name, money(discountAmount), expiresAt));
        coupon.updateOrderCoupon(name, money(discountAmount), expiresAt);

        return coupon;
    }

    private Coupon productCoupon(String name, String discountAmount, Product targetProduct, LocalDateTime expiresAt) {
        Coupon coupon = couponRepository.findFirstByNameOrderByIdAsc(name)
                .orElseGet(() -> Coupon.createProductCoupon(name, money(discountAmount), targetProduct, expiresAt));
        coupon.updateProductCoupon(name, money(discountAmount), targetProduct, expiresAt);

        return coupon;
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}

package com.ecommerce.coupon.entity;

import com.ecommerce.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponType type;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_product_id")
    private Product targetProduct;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "first_order_only", nullable = false)
    private boolean firstOrderOnly;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Coupon() {
    }

    private Coupon(
            String name,
            CouponType type,
            BigDecimal discountAmount,
            Product targetProduct,
            LocalDateTime expiresAt,
            boolean firstOrderOnly
    ) {
        this.name = requireText(name, "쿠폰명");
        this.type = Objects.requireNonNull(type, "쿠폰 타입은 필수입니다.");
        this.discountAmount = requirePositiveDiscountAmount(discountAmount);
        this.targetProduct = targetProduct;
        this.expiresAt = Objects.requireNonNull(expiresAt, "쿠폰 만료 시간은 필수입니다.");
        this.firstOrderOnly = firstOrderOnly;
        validateTargetProduct();
    }

    public static Coupon createOrderCoupon(String name, BigDecimal discountAmount, LocalDateTime expiresAt) {
        return new Coupon(name, CouponType.ORDER, discountAmount, null, expiresAt, false);
    }

    public static Coupon createFirstOrderCoupon(String name, BigDecimal discountAmount, LocalDateTime expiresAt) {
        return new Coupon(name, CouponType.ORDER, discountAmount, null, expiresAt, true);
    }

    public static Coupon createProductCoupon(
            String name,
            BigDecimal discountAmount,
            Product targetProduct,
            LocalDateTime expiresAt
    ) {
        return new Coupon(
                name,
                CouponType.PRODUCT,
                discountAmount,
                Objects.requireNonNull(targetProduct, "쿠폰 대상 상품은 필수입니다."),
                expiresAt,
                false
        );
    }

    public boolean isExpired(LocalDateTime now) {
        return Objects.requireNonNull(now, "현재 시간은 필수입니다.").isAfter(expiresAt);
    }

    public boolean isOrderCoupon() {
        return type == CouponType.ORDER;
    }

    public boolean isFirstOrderOnly() {
        return firstOrderOnly;
    }

    public boolean isEligibleForCustomer(boolean hasCompletedOrder) {
        return !firstOrderOnly || !hasCompletedOrder;
    }

    public boolean isProductCouponFor(Product product) {
        return type == CouponType.PRODUCT
                && targetProduct != null
                && Objects.equals(targetProduct.getId(), Objects.requireNonNull(product, "상품은 필수입니다.").getId());
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CouponType getType() {
        return type;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public Product getTargetProduct() {
        return targetProduct;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }

        return value.trim();
    }

    private static BigDecimal requirePositiveDiscountAmount(BigDecimal discountAmount) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("쿠폰 할인 금액은 0보다 커야 합니다.");
        }

        return discountAmount;
    }

    private void validateTargetProduct() {
        if (type == CouponType.ORDER && targetProduct != null) {
            throw new IllegalArgumentException("전체 상품 쿠폰은 대상 상품을 가질 수 없습니다.");
        }
        if (type == CouponType.PRODUCT && targetProduct == null) {
            throw new IllegalArgumentException("특정 상품 쿠폰은 대상 상품이 필요합니다.");
        }
        if (firstOrderOnly && type != CouponType.ORDER) {
            throw new IllegalArgumentException("첫 주문 전용 쿠폰은 전체 상품 쿠폰이어야 합니다.");
        }
    }
}

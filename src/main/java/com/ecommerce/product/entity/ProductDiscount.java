package com.ecommerce.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "product_discounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_discounts_product_id", columnNames = "product_id")
)
public class ProductDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ProductDiscount() {
    }

    private ProductDiscount(
            Product product,
            String name,
            BigDecimal discountAmount,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) {
        this.product = Objects.requireNonNull(product, "할인 대상 상품은 필수입니다.");
        update(name, discountAmount, startsAt, endsAt);
        this.active = true;
    }

    public static ProductDiscount create(
            Product product,
            String name,
            BigDecimal discountAmount,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) {
        return new ProductDiscount(product, name, discountAmount, startsAt, endsAt);
    }

    public void update(
            String name,
            BigDecimal discountAmount,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) {
        this.name = requireText(name, "상품 즉시 할인명");
        this.discountAmount = requirePositiveDiscountAmount(discountAmount);
        this.startsAt = Objects.requireNonNull(startsAt, "할인 시작 시간은 필수입니다.");
        this.endsAt = Objects.requireNonNull(endsAt, "할인 종료 시간은 필수입니다.");
        validatePeriod(this.startsAt, this.endsAt);
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isApplicableAt(LocalDateTime now) {
        LocalDateTime targetTime = Objects.requireNonNull(now, "현재 시간은 필수입니다.");

        return active && !targetTime.isBefore(startsAt) && !targetTime.isAfter(endsAt);
    }

    public BigDecimal getApplicableUnitDiscountAmount(LocalDateTime now, BigDecimal productPrice) {
        if (!isApplicableAt(now)) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxDiscountAmount = Objects.requireNonNull(productPrice, "상품 가격은 필수입니다.");
        if (discountAmount.compareTo(maxDiscountAmount) > 0) {
            return maxDiscountAmount;
        }

        return discountAmount;
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

    public Product getProduct() {
        return product;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public boolean isActive() {
        return active;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }

        return value.trim();
    }

    private static BigDecimal requirePositiveDiscountAmount(BigDecimal discountAmount) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("상품 즉시 할인 금액은 0보다 커야 합니다.");
        }

        return discountAmount;
    }

    private static void validatePeriod(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("할인 종료 시간은 시작 시간보다 늦어야 합니다.");
        }
    }
}

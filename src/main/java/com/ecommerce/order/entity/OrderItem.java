package com.ecommerce.order.entity;

import com.ecommerce.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "product_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal productPrice;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "source_cart_item_id")
    private Long sourceCartItemId;

    @Column(name = "original_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "instant_discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal instantDiscountAmount;

    @Column(name = "product_coupon_discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal productCouponDiscountAmount;

    @Column(name = "final_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected OrderItem() {
    }

    private OrderItem(Product product, long quantity, Long sourceCartItemId, BigDecimal productCouponDiscountAmount) {
        Product targetProduct = Objects.requireNonNull(product, "주문 상품은 필수입니다.");
        this.product = targetProduct;
        this.productName = targetProduct.getName();
        this.productPrice = targetProduct.getPrice();
        this.quantity = requirePositiveQuantity(quantity);
        this.sourceCartItemId = normalizeSourceCartItemId(sourceCartItemId);
        this.originalAmount = this.productPrice.multiply(BigDecimal.valueOf(this.quantity));
        this.instantDiscountAmount = BigDecimal.ZERO;
        this.productCouponDiscountAmount = requireNonNegativeAmount(productCouponDiscountAmount, "상품 쿠폰 할인 금액");
        this.finalAmount = originalAmount
                .subtract(this.instantDiscountAmount)
                .subtract(this.productCouponDiscountAmount)
                .max(BigDecimal.ZERO);
    }

    public static OrderItem create(Product product, long quantity) {
        return new OrderItem(product, quantity, null, BigDecimal.ZERO);
    }

    public static OrderItem create(Product product, long quantity, Long sourceCartItemId) {
        return new OrderItem(product, quantity, sourceCartItemId, BigDecimal.ZERO);
    }

    public static OrderItem create(
            Product product,
            long quantity,
            Long sourceCartItemId,
            BigDecimal productCouponDiscountAmount
    ) {
        return new OrderItem(product, quantity, sourceCartItemId, productCouponDiscountAmount);
    }

    void assignOrder(Order order) {
        this.order = Objects.requireNonNull(order, "주문은 필수입니다.");
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

    public Order getOrder() {
        return order;
    }

    public Product getProduct() {
        return product;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public long getQuantity() {
        return quantity;
    }

    public Long getSourceCartItemId() {
        return sourceCartItemId;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public BigDecimal getInstantDiscountAmount() {
        return instantDiscountAmount;
    }

    public BigDecimal getProductCouponDiscountAmount() {
        return productCouponDiscountAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    private static long requirePositiveQuantity(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문 상품 수량은 1 이상이어야 합니다.");
        }

        return quantity;
    }

    private static Long normalizeSourceCartItemId(Long sourceCartItemId) {
        if (sourceCartItemId == null) {
            return null;
        }
        if (sourceCartItemId <= 0) {
            throw new IllegalArgumentException("원본 장바구니 상품 ID는 1 이상이어야 합니다.");
        }

        return sourceCartItemId;
    }

    private static BigDecimal requireNonNegativeAmount(BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + "은(는) 0 이상이어야 합니다.");
        }

        return amount;
    }
}

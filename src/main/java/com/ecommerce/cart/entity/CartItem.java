package com.ecommerce.cart.entity;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_cart_items_cart_product", columnNames = {"cart_id", "product_id"})
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CartItem() {
    }

    private CartItem(Cart cart, Product product, long quantity) {
        this.cart = Objects.requireNonNull(cart, "장바구니는 필수입니다.");
        this.product = Objects.requireNonNull(product, "장바구니 상품은 필수입니다.");
        changeQuantity(quantity);
    }

    public static CartItem create(Cart cart, Product product, long quantity) {
        return new CartItem(cart, product, quantity);
    }

    public void increaseQuantity(long quantity) {
        changeQuantity(Math.addExact(this.quantity, requirePositiveQuantity(quantity)));
    }

    public void changeQuantity(long quantity) {
        this.quantity = requirePositiveQuantity(quantity);
    }

    public boolean isSelectable() {
        return getNotSelectableReason() == null;
    }

    public String getNotSelectableReason() {
        if (product.getStatus() != ProductStatus.ON_SALE) {
            return "PRODUCT_NOT_ON_SALE";
        }
        if (!product.getStock().hasEnough(quantity)) {
            return "OUT_OF_STOCK";
        }

        return null;
    }

    void detachCart() {
        this.cart = null;
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

    public Cart getCart() {
        return cart;
    }

    public Product getProduct() {
        return product;
    }

    public long getQuantity() {
        return quantity;
    }

    private static long requirePositiveQuantity(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("장바구니 상품 수량은 1 이상이어야 합니다.");
        }

        return quantity;
    }
}

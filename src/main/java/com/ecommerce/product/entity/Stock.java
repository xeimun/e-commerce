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
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Stock() {
    }

    private Stock(Product product, long quantity) {
        this.product = Objects.requireNonNull(product, "재고 대상 상품은 필수입니다.");
        setQuantity(quantity);
    }

    public static Stock create(Product product, long quantity) {
        return new Stock(product, quantity);
    }

    public void setQuantity(long quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }

        this.quantity = quantity;
    }

    public boolean hasEnough(long quantity) {
        return this.quantity >= quantity;
    }

    public void decrease(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감할 재고 수량은 0보다 커야 합니다.");
        }
        if (!hasEnough(quantity)) {
            throw new IllegalArgumentException("상품 재고가 부족합니다.");
        }

        this.quantity -= quantity;
    }

    public void increase(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("복구할 재고 수량은 0보다 커야 합니다.");
        }

        this.quantity += quantity;
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

    public long getQuantity() {
        return quantity;
    }
}

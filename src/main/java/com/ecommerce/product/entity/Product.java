package com.ecommerce.product.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "content_title", nullable = false, length = 100)
    private String contentTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private ContentType contentType;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 1000)
    private String description;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Stock stock;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Product() {
    }

    private Product(
            String name,
            BigDecimal price,
            ProductStatus status,
            String contentTitle,
            ContentType contentType,
            String category,
            String description
    ) {
        this.name = requireText(name, "상품명");
        this.price = requirePositivePrice(price);
        this.status = Objects.requireNonNullElse(status, ProductStatus.ON_SALE);
        this.contentTitle = requireText(contentTitle, "콘텐츠명");
        this.contentType = Objects.requireNonNull(contentType, "콘텐츠 유형은 필수입니다.");
        this.category = requireText(category, "상품 분류");
        this.description = normalizeDescription(description);
    }

    public static Product create(
            String name,
            BigDecimal price,
            ProductStatus status,
            String contentTitle,
            ContentType contentType,
            String category,
            String description
    ) {
        return new Product(name, price, status, contentTitle, contentType, category, description);
    }

    public void update(
            String name,
            BigDecimal price,
            String contentTitle,
            ContentType contentType,
            String category,
            String description
    ) {
        this.name = requireText(name, "상품명");
        this.price = requirePositivePrice(price);
        this.contentTitle = requireText(contentTitle, "콘텐츠명");
        this.contentType = Objects.requireNonNull(contentType, "콘텐츠 유형은 필수입니다.");
        this.category = requireText(category, "상품 분류");
        this.description = normalizeDescription(description);
    }

    public void changeStatus(ProductStatus status) {
        this.status = Objects.requireNonNull(status, "상품 상태는 필수입니다.");
    }

    public void registerStock(Stock stock) {
        this.stock = Objects.requireNonNull(stock, "상품 재고는 필수입니다.");
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

    public BigDecimal getPrice() {
        return price;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public String getContentTitle() {
        return contentTitle;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Stock getStock() {
        return stock;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }

        return value.trim();
    }

    private static BigDecimal requirePositivePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("상품 가격은 0보다 커야 합니다.");
        }

        return price;
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}

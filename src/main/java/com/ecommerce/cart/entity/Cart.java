package com.ecommerce.cart.entity;

import com.ecommerce.product.entity.Product;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(
        name = "carts",
        uniqueConstraints = @UniqueConstraint(name = "uk_carts_customer_id", columnNames = "customer_id")
)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Cart() {
    }

    private Cart(Long customerId) {
        this.customerId = requirePositiveCustomerId(customerId);
    }

    public static Cart create(Long customerId) {
        return new Cart(customerId);
    }

    public CartItem addItem(Product product, long quantity) {
        Optional<CartItem> existingItem = findItemByProductId(product.getId());
        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            cartItem.increaseQuantity(quantity);
            return cartItem;
        }

        CartItem cartItem = CartItem.create(this, product, quantity);
        items.add(cartItem);

        return cartItem;
    }

    public Optional<CartItem> findItemById(Long cartItemId) {
        return items.stream()
                .filter(item -> Objects.equals(item.getId(), cartItemId))
                .findFirst();
    }

    public Optional<CartItem> findItemByProductId(Long productId) {
        return items.stream()
                .filter(item -> Objects.equals(item.getProduct().getId(), productId))
                .findFirst();
    }

    public void removeItem(CartItem cartItem) {
        if (items.remove(cartItem)) {
            cartItem.detachCart();
        }
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

    public Long getCustomerId() {
        return customerId;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    private static Long requirePositiveCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("고객 ID는 1 이상이어야 합니다.");
        }

        return customerId;
    }
}

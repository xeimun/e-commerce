package com.ecommerce.cart.repository;

import com.ecommerce.cart.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.stock", "items.product.productDiscount"})
    Optional<Cart> findByCustomerId(Long customerId);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.productDiscount"})
    @Query("select distinct c from Cart c where c.customerId = :customerId")
    Optional<Cart> findForOrderByCustomerId(@Param("customerId") Long customerId);

    void deleteByCustomerId(Long customerId);
}

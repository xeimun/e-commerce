package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"stock", "productDiscount"})
    List<Product> findAllByOrderByIdAsc();

    @Override
    @EntityGraph(attributePaths = {"stock", "productDiscount"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"stock", "productDiscount"})
    Optional<Product> findFirstByNameOrderByIdAsc(String name);
}

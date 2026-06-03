package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findAllByCustomerIdOrderByIdDesc(Long customerId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);
}

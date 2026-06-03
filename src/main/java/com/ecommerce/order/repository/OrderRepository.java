package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findAllByCustomerIdOrderByIdDesc(Long customerId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("select o from Order o where o.id = :id and o.customerId = :customerId")
    Optional<Order> findByIdAndCustomerIdForUpdate(@Param("id") Long id, @Param("customerId") Long customerId);
}

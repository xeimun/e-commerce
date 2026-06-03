package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Stock;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.product.id in :productIds")
    List<Stock> findAllByProductIdInForUpdate(@Param("productIds") Collection<Long> productIds);
}

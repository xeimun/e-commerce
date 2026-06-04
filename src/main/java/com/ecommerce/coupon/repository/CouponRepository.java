package com.ecommerce.coupon.repository;

import com.ecommerce.coupon.entity.Coupon;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @EntityGraph(attributePaths = "targetProduct")
    List<Coupon> findAllByExpiresAtGreaterThanEqualOrderByIdAsc(LocalDateTime now);
}

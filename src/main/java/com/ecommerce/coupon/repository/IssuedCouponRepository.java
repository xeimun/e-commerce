package com.ecommerce.coupon.repository;

import com.ecommerce.coupon.entity.IssuedCoupon;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {

    boolean existsByCouponIdAndCustomerId(Long couponId, Long customerId);

    @EntityGraph(attributePaths = {"coupon", "coupon.targetProduct"})
    List<IssuedCoupon> findAllByCustomerIdOrderByIdDesc(Long customerId);

    @Query("select ic.coupon.id from IssuedCoupon ic where ic.customerId = :customerId and ic.coupon.id in :couponIds")
    List<Long> findIssuedCouponIdsByCustomerIdAndCouponIdIn(
            @Param("customerId") Long customerId,
            @Param("couponIds") Collection<Long> couponIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"coupon", "coupon.targetProduct"})
    @Query("select ic from IssuedCoupon ic where ic.id in :ids")
    List<IssuedCoupon> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"coupon", "coupon.targetProduct"})
    @Query("select ic from IssuedCoupon ic where ic.order.id = :orderId")
    List<IssuedCoupon> findAllByOrderIdForUpdate(@Param("orderId") Long orderId);

    void deleteAllByCustomerId(Long customerId);
}

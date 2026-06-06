package com.ecommerce.coupon.service;

import com.ecommerce.coupon.dto.IssuableCouponResponse;
import com.ecommerce.coupon.dto.IssuedCouponIssueResponse;
import com.ecommerce.coupon.dto.MyCouponResponse;
import com.ecommerce.coupon.entity.Coupon;
import com.ecommerce.coupon.entity.IssuedCoupon;
import com.ecommerce.coupon.exception.CouponAlreadyIssuedException;
import com.ecommerce.coupon.exception.CouponExpiredException;
import com.ecommerce.coupon.exception.CouponNotFoundException;
import com.ecommerce.coupon.repository.CouponRepository;
import com.ecommerce.coupon.repository.IssuedCouponRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    public CouponService(CouponRepository couponRepository, IssuedCouponRepository issuedCouponRepository) {
        this.couponRepository = couponRepository;
        this.issuedCouponRepository = issuedCouponRepository;
    }

    public List<IssuableCouponResponse> getIssuableCoupons(Long customerId) {
        Long validCustomerId = requirePositiveCustomerId(customerId);
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = couponRepository.findAllByExpiresAtGreaterThanEqualOrderByIdAsc(now);
        Set<Long> issuedCouponIds = findIssuedCouponIds(validCustomerId, coupons);

        return coupons.stream()
                .map(coupon -> IssuableCouponResponse.from(coupon, !issuedCouponIds.contains(coupon.getId())))
                .toList();
    }

    @Transactional
    public IssuedCouponIssueResponse issueCoupon(Long customerId, Long couponId) {
        Long validCustomerId = requirePositiveCustomerId(customerId);
        Long validCouponId = requirePositiveCouponId(couponId);
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = couponRepository.findById(validCouponId)
                .orElseThrow(() -> new CouponNotFoundException(validCouponId));
        if (coupon.isExpired(now)) {
            throw new CouponExpiredException(validCouponId);
        }
        if (issuedCouponRepository.existsByCouponIdAndCustomerId(validCouponId, validCustomerId)) {
            throw new CouponAlreadyIssuedException(validCouponId);
        }

        try {
            IssuedCoupon issuedCoupon = issuedCouponRepository.saveAndFlush(
                    IssuedCoupon.issue(coupon, validCustomerId, now)
            );

            return IssuedCouponIssueResponse.from(issuedCoupon);
        } catch (DataIntegrityViolationException exception) {
            throw new CouponAlreadyIssuedException(validCouponId);
        }
    }

    public List<MyCouponResponse> getMyCoupons(Long customerId) {
        Long validCustomerId = requirePositiveCustomerId(customerId);
        LocalDateTime now = LocalDateTime.now();

        return issuedCouponRepository.findAllByCustomerIdOrderByIdDesc(validCustomerId)
                .stream()
                .map(issuedCoupon -> MyCouponResponse.from(issuedCoupon, now))
                .toList();
    }

    private Set<Long> findIssuedCouponIds(Long customerId, List<Coupon> coupons) {
        List<Long> couponIds = coupons.stream()
                .map(Coupon::getId)
                .toList();
        if (couponIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(issuedCouponRepository.findIssuedCouponIdsByCustomerIdAndCouponIdIn(customerId, couponIds));
    }

    private static Long requirePositiveCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("고객 ID는 1 이상이어야 합니다.");
        }

        return customerId;
    }

    private static Long requirePositiveCouponId(Long couponId) {
        if (couponId == null || couponId <= 0) {
            throw new IllegalArgumentException("쿠폰 ID는 1 이상이어야 합니다.");
        }

        return couponId;
    }
}

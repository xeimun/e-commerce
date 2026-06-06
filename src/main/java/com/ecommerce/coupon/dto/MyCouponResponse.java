package com.ecommerce.coupon.dto;

import com.ecommerce.coupon.entity.Coupon;
import com.ecommerce.coupon.entity.CouponType;
import com.ecommerce.coupon.entity.IssuedCoupon;
import com.ecommerce.coupon.entity.IssuedCouponStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyCouponResponse(
        Long issuedCouponId,
        Long couponId,
        String name,
        CouponType type,
        BigDecimal discountAmount,
        Long targetProductId,
        boolean firstOrderOnly,
        IssuedCouponStatus status,
        LocalDateTime expiresAt
) {

    public static MyCouponResponse from(IssuedCoupon issuedCoupon, LocalDateTime now) {
        Coupon coupon = issuedCoupon.getCoupon();
        Long targetProductId = coupon.getTargetProduct() == null ? null : coupon.getTargetProduct().getId();

        return new MyCouponResponse(
                issuedCoupon.getId(),
                coupon.getId(),
                coupon.getName(),
                coupon.getType(),
                coupon.getDiscountAmount(),
                targetProductId,
                coupon.isFirstOrderOnly(),
                effectiveStatus(issuedCoupon, now),
                coupon.getExpiresAt()
        );
    }

    private static IssuedCouponStatus effectiveStatus(IssuedCoupon issuedCoupon, LocalDateTime now) {
        if (issuedCoupon.getStatus() == IssuedCouponStatus.AVAILABLE && issuedCoupon.getCoupon().isExpired(now)) {
            return IssuedCouponStatus.EXPIRED;
        }

        return issuedCoupon.getStatus();
    }
}

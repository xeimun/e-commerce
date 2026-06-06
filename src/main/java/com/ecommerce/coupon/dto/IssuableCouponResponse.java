package com.ecommerce.coupon.dto;

import com.ecommerce.coupon.entity.Coupon;
import com.ecommerce.coupon.entity.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IssuableCouponResponse(
        Long couponId,
        String name,
        CouponType type,
        BigDecimal discountAmount,
        Long targetProductId,
        boolean firstOrderOnly,
        LocalDateTime expiresAt,
        boolean issuable
) {

    public static IssuableCouponResponse from(Coupon coupon, boolean issuable) {
        Long targetProductId = coupon.getTargetProduct() == null ? null : coupon.getTargetProduct().getId();

        return new IssuableCouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getType(),
                coupon.getDiscountAmount(),
                targetProductId,
                coupon.isFirstOrderOnly(),
                coupon.getExpiresAt(),
                issuable
        );
    }
}

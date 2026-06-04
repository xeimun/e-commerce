package com.ecommerce.coupon.dto;

import com.ecommerce.coupon.entity.IssuedCoupon;
import com.ecommerce.coupon.entity.IssuedCouponStatus;
import java.time.LocalDateTime;

public record IssuedCouponIssueResponse(
        Long issuedCouponId,
        Long couponId,
        IssuedCouponStatus status,
        LocalDateTime issuedAt
) {

    public static IssuedCouponIssueResponse from(IssuedCoupon issuedCoupon) {
        return new IssuedCouponIssueResponse(
                issuedCoupon.getId(),
                issuedCoupon.getCoupon().getId(),
                issuedCoupon.getStatus(),
                issuedCoupon.getIssuedAt()
        );
    }
}

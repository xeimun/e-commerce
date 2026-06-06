package com.ecommerce.coupon.exception;

public class CouponNotIssuableException extends RuntimeException {

    private final Long couponId;

    public CouponNotIssuableException(Long couponId) {
        super("발급이 중단된 쿠폰입니다. couponId=" + couponId);
        this.couponId = couponId;
    }

    public Long getCouponId() {
        return couponId;
    }
}

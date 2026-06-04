package com.ecommerce.coupon.exception;

public class CouponExpiredException extends RuntimeException {

    private final Long couponId;

    public CouponExpiredException(Long couponId) {
        super("만료된 쿠폰입니다. couponId=" + couponId);
        this.couponId = couponId;
    }

    public Long getCouponId() {
        return couponId;
    }
}

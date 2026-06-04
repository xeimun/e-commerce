package com.ecommerce.coupon.exception;

public class CouponNotFoundException extends RuntimeException {

    private final Long couponId;

    public CouponNotFoundException(Long couponId) {
        super("쿠폰을 찾을 수 없습니다. couponId=" + couponId);
        this.couponId = couponId;
    }

    public Long getCouponId() {
        return couponId;
    }
}

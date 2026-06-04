package com.ecommerce.coupon.exception;

public class CouponAlreadyIssuedException extends RuntimeException {

    private final Long couponId;

    public CouponAlreadyIssuedException(Long couponId) {
        super("이미 발급받은 쿠폰입니다. couponId=" + couponId);
        this.couponId = couponId;
    }

    public Long getCouponId() {
        return couponId;
    }
}

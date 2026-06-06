package com.ecommerce.coupon.exception;

public class FirstOrderCouponNotAvailableException extends RuntimeException {

    private final Long couponId;

    public FirstOrderCouponNotAvailableException(Long couponId) {
        super("첫 주문 전용 쿠폰을 발급할 수 없습니다. couponId=" + couponId);
        this.couponId = couponId;
    }

    public Long getCouponId() {
        return couponId;
    }
}

package com.ecommerce.order.exception;

public class OrderPaymentExpiredException extends RuntimeException {

    private final Long orderId;

    public OrderPaymentExpiredException(Long orderId) {
        super("결제 대기 시간이 만료되었습니다. orderId=" + orderId);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}

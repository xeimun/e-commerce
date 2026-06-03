package com.ecommerce.order.exception;

public class OrderNotPaymentPendingException extends RuntimeException {

    private final Long orderId;

    public OrderNotPaymentPendingException(Long orderId) {
        super("결제 대기 상태의 주문만 처리할 수 있습니다. orderId=" + orderId);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}

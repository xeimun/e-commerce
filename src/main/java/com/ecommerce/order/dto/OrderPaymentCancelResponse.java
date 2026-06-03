package com.ecommerce.order.dto;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderCancelReason;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentStatus;

public record OrderPaymentCancelResponse(
        Long orderId,
        OrderStatus status,
        PaymentStatus paymentStatus,
        OrderCancelReason cancelReason
) {

    public static OrderPaymentCancelResponse from(Order order) {
        return new OrderPaymentCancelResponse(
                order.getId(),
                order.getStatus(),
                PaymentStatus.CANCELED,
                order.getCancelReason()
        );
    }
}

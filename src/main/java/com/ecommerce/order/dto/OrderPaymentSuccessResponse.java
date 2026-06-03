package com.ecommerce.order.dto;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderPaymentSuccessResponse(
        Long orderId,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal finalPaymentAmount,
        LocalDateTime paidAt
) {

    public static OrderPaymentSuccessResponse from(Order order) {
        return new OrderPaymentSuccessResponse(
                order.getId(),
                order.getStatus(),
                PaymentStatus.PAID,
                order.getFinalPaymentAmount(),
                order.getPaidAt()
        );
    }
}

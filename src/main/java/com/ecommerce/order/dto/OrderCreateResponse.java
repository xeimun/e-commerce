package com.ecommerce.order.dto;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreateResponse(
        Long orderId,
        OrderStatus status,
        LocalDateTime expiresAt,
        BigDecimal totalProductAmount,
        BigDecimal totalInstantDiscountAmount,
        BigDecimal totalCouponDiscountAmount,
        BigDecimal finalPaymentAmount
) {

    public static OrderCreateResponse from(Order order) {
        return new OrderCreateResponse(
                order.getId(),
                order.getStatus(),
                order.getExpiresAt(),
                order.getTotalProductAmount(),
                order.getTotalInstantDiscountAmount(),
                order.getTotalCouponDiscountAmount(),
                order.getFinalPaymentAmount()
        );
    }
}

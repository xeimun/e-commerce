package com.ecommerce.order.dto;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderCancelReason;
import com.ecommerce.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        Long orderId,
        OrderStatus status,
        LocalDateTime expiresAt,
        OrderCancelReason cancelReason,
        BigDecimal totalProductAmount,
        BigDecimal totalInstantDiscountAmount,
        BigDecimal totalCouponDiscountAmount,
        BigDecimal finalPaymentAmount,
        List<OrderItemResponse> items
) {

    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(
                order.getId(),
                order.getStatus(),
                order.getExpiresAt(),
                order.getCancelReason(),
                order.getTotalProductAmount(),
                order.getTotalInstantDiscountAmount(),
                order.getTotalCouponDiscountAmount(),
                order.getFinalPaymentAmount(),
                order.getItems()
                        .stream()
                        .map(OrderItemResponse::from)
                        .toList()
        );
    }
}

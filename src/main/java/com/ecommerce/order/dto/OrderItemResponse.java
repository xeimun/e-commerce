package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        String productName,
        BigDecimal productPrice,
        long quantity,
        BigDecimal originalAmount,
        BigDecimal instantDiscountAmount,
        BigDecimal productCouponDiscountAmount,
        BigDecimal finalAmount
) {

    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProduct().getId(),
                orderItem.getProductName(),
                orderItem.getProductPrice(),
                orderItem.getQuantity(),
                orderItem.getOriginalAmount(),
                orderItem.getInstantDiscountAmount(),
                orderItem.getProductCouponDiscountAmount(),
                orderItem.getFinalAmount()
        );
    }
}

package com.ecommerce.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetail(
        String targetType,
        Long targetId,
        String reason,
        String message,
        Long currentStock
) {

    public static ErrorDetail product(Long productId, String reason) {
        return new ErrorDetail("PRODUCT", productId, reason, null, null);
    }

    public static ErrorDetail field(String fieldName, String message) {
        return new ErrorDetail("FIELD", null, fieldName, message, null);
    }

    public static ErrorDetail productStock(Long productId, String reason, long currentStock) {
        return new ErrorDetail("PRODUCT", productId, reason, null, currentStock);
    }
}

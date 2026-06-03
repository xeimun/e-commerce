package com.ecommerce.common.exception;

import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<ErrorDetail> details
) {

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, List.of());
    }

    public static ApiErrorResponse of(String code, String message, List<ErrorDetail> details) {
        return new ApiErrorResponse(code, message, details);
    }
}

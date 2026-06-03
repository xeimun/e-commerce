package com.ecommerce.order.exception;

import com.ecommerce.common.exception.ErrorDetail;
import java.util.List;

public class OrderValidationException extends RuntimeException {

    private final List<ErrorDetail> details;

    public OrderValidationException(List<ErrorDetail> details) {
        super("주문할 수 없는 상품 또는 쿠폰이 포함되어 있습니다.");
        this.details = List.copyOf(details);
    }

    public List<ErrorDetail> getDetails() {
        return details;
    }
}

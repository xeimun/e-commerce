package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotNull;

public record OrderProductCouponRequest(
        @NotNull
        Long cartItemId,

        @NotNull
        Long couponId
) {
}

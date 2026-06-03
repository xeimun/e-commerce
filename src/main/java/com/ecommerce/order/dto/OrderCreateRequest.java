package com.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderCreateRequest(
        @NotEmpty
        List<@NotNull Long> cartItemIds,

        Long orderCouponId,

        List<@Valid @NotNull OrderProductCouponRequest> productCoupons
) {
}

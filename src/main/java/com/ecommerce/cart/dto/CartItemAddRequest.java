package com.ecommerce.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemAddRequest(
        @NotNull
        Long productId,

        @NotNull
        @Min(1)
        Long quantity
) {
}

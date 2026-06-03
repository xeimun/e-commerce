package com.ecommerce.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemQuantityUpdateRequest(
        @NotNull
        @Min(1)
        Long quantity
) {
}

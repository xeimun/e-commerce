package com.ecommerce.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductStockUpdateRequest(
        @NotNull
        @Min(0)
        Long stockQuantity
) {
}

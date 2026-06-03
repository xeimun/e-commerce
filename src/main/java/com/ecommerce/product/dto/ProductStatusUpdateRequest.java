package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusUpdateRequest(
        @NotNull ProductStatus status
) {
}

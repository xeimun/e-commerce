package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ContentType;
import com.ecommerce.product.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 17, fraction = 2)
        BigDecimal price,

        ProductStatus status,

        @NotBlank
        @Size(max = 100)
        String contentTitle,

        @NotNull
        ContentType contentType,

        @NotBlank
        @Size(max = 50)
        String category,

        @Size(max = 1000)
        String description,

        @NotNull
        @Min(0)
        Long stockQuantity
) {
}

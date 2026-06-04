package com.ecommerce.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductInstantDiscountRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 17, fraction = 2)
        BigDecimal discountAmount,

        @NotNull
        LocalDateTime startsAt,

        @NotNull
        LocalDateTime endsAt
) {
}

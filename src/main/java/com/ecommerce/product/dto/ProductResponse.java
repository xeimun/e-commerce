package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ContentType;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import java.math.BigDecimal;

public record ProductResponse(
        Long productId,
        String name,
        BigDecimal price,
        ProductStatus status,
        String contentTitle,
        ContentType contentType,
        String category,
        String description,
        long stockQuantity,
        BigDecimal instantDiscountAmount
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStatus(),
                product.getContentTitle(),
                product.getContentType(),
                product.getCategory(),
                product.getDescription(),
                product.getStock().getQuantity(),
                BigDecimal.ZERO
        );
    }
}

package com.ecommerce.cart.dto;

import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import java.math.BigDecimal;

public record CartItemResponse(
        Long cartItemId,
        Long productId,
        String productName,
        BigDecimal price,
        long quantity,
        long stockQuantity,
        ProductStatus productStatus,
        boolean selectable,
        String notSelectableReason
) {

    public static CartItemResponse from(CartItem cartItem) {
        Product product = cartItem.getProduct();

        return new CartItemResponse(
                cartItem.getId(),
                product.getId(),
                product.getName(),
                product.getPrice(),
                cartItem.getQuantity(),
                product.getStock().getQuantity(),
                product.getStatus(),
                cartItem.isSelectable(),
                cartItem.getNotSelectableReason()
        );
    }
}

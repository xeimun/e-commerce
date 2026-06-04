package com.ecommerce.cart.dto;

import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartItemResponse(
        Long cartItemId,
        Long productId,
        String productName,
        BigDecimal price,
        BigDecimal instantDiscountAmount,
        BigDecimal discountedPrice,
        long quantity,
        long stockQuantity,
        ProductStatus productStatus,
        boolean selectable,
        String notSelectableReason
) {

    public static CartItemResponse from(CartItem cartItem) {
        return from(cartItem, LocalDateTime.now());
    }

    public static CartItemResponse from(CartItem cartItem, LocalDateTime now) {
        Product product = cartItem.getProduct();
        BigDecimal instantDiscountAmount = product.getInstantDiscountAmount(now);
        BigDecimal discountedPrice = product.getPrice().subtract(instantDiscountAmount);

        return new CartItemResponse(
                cartItem.getId(),
                product.getId(),
                product.getName(),
                product.getPrice(),
                instantDiscountAmount,
                discountedPrice,
                cartItem.getQuantity(),
                product.getStock().getQuantity(),
                product.getStatus(),
                cartItem.isSelectable(),
                cartItem.getNotSelectableReason()
        );
    }
}

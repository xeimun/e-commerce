package com.ecommerce.cart.dto;

import com.ecommerce.cart.entity.CartItem;

public record CartItemMutationResponse(
        Long cartItemId,
        Long productId,
        long quantity
) {

    public static CartItemMutationResponse from(CartItem cartItem) {
        return new CartItemMutationResponse(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getQuantity()
        );
    }
}

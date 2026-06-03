package com.ecommerce.cart.dto;

import com.ecommerce.cart.entity.Cart;
import java.util.List;

public record CartResponse(
        Long cartId,
        List<CartItemResponse> items
) {

    public static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getItems()
                        .stream()
                        .map(CartItemResponse::from)
                        .toList()
        );
    }
}

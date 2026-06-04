package com.ecommerce.cart.dto;

import com.ecommerce.cart.entity.Cart;
import java.time.LocalDateTime;
import java.util.List;

public record CartResponse(
        Long cartId,
        List<CartItemResponse> items
) {

    public static CartResponse from(Cart cart) {
        LocalDateTime now = LocalDateTime.now();

        return new CartResponse(
                cart.getId(),
                cart.getItems()
                        .stream()
                        .map(cartItem -> CartItemResponse.from(cartItem, now))
                        .toList()
        );
    }
}

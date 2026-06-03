package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.CartItemAddRequest;
import com.ecommerce.cart.dto.CartItemMutationResponse;
import com.ecommerce.cart.dto.CartItemQuantityUpdateRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(@RequestHeader("X-Customer-Id") Long customerId) {
        return cartService.getCart(customerId);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemMutationResponse addItem(
            @RequestHeader("X-Customer-Id") Long customerId,
            @Valid @RequestBody CartItemAddRequest request
    ) {
        return cartService.addItem(customerId, request);
    }

    @PatchMapping("/items/{cartItemId}")
    public CartItemMutationResponse updateItemQuantity(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemQuantityUpdateRequest request
    ) {
        return cartService.updateItemQuantity(customerId, cartItemId, request);
    }

    @DeleteMapping("/items/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@RequestHeader("X-Customer-Id") Long customerId, @PathVariable Long cartItemId) {
        cartService.deleteItem(customerId, cartItemId);
    }
}

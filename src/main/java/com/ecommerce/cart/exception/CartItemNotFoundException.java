package com.ecommerce.cart.exception;

public class CartItemNotFoundException extends RuntimeException {

    private final Long customerId;
    private final Long cartItemId;

    public CartItemNotFoundException(Long customerId, Long cartItemId) {
        super("장바구니 상품을 찾을 수 없습니다. customerId=" + customerId + ", cartItemId=" + cartItemId);
        this.customerId = customerId;
        this.cartItemId = cartItemId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getCartItemId() {
        return cartItemId;
    }
}

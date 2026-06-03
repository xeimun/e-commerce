package com.ecommerce.cart.exception;

public class CartItemQuantityExceededException extends RuntimeException {

    private final Long productId;
    private final long requestedQuantity;
    private final long currentStock;

    public CartItemQuantityExceededException(Long productId, long requestedQuantity, long currentStock) {
        super("장바구니 상품 수량은 현재 재고보다 클 수 없습니다. productId="
                + productId + ", requestedQuantity=" + requestedQuantity + ", currentStock=" + currentStock);
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.currentStock = currentStock;
    }

    public Long getProductId() {
        return productId;
    }

    public long getRequestedQuantity() {
        return requestedQuantity;
    }

    public long getCurrentStock() {
        return currentStock;
    }
}

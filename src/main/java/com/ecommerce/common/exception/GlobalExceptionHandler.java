package com.ecommerce.common.exception;

import com.ecommerce.cart.exception.CartItemNotFoundException;
import com.ecommerce.cart.exception.CartItemQuantityExceededException;
import com.ecommerce.product.exception.ProductNotFoundException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound(ProductNotFoundException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "PRODUCT_NOT_FOUND",
                "상품을 찾을 수 없습니다.",
                List.of(ErrorDetail.product(exception.getProductId(), "PRODUCT_NOT_FOUND"))
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCartItemNotFound(CartItemNotFoundException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "CART_ITEM_NOT_FOUND",
                "장바구니 상품을 찾을 수 없습니다.",
                List.of(ErrorDetail.cartItem(exception.getCartItemId(), "CART_ITEM_NOT_FOUND"))
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(CartItemQuantityExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleCartItemQuantityExceeded(CartItemQuantityExceededException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "OUT_OF_STOCK",
                "상품 재고가 부족합니다.",
                List.of(ErrorDetail.productStock(exception.getProductId(), "OUT_OF_STOCK", exception.getCurrentStock()))
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<ErrorDetail> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toErrorDetail)
                .toList();

        ApiErrorResponse response = ApiErrorResponse.of(
                "INVALID_REQUEST",
                "요청 값이 올바르지 않습니다.",
                details
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        ApiErrorResponse response = ApiErrorResponse.of("INVALID_REQUEST", exception.getMessage());

        return ResponseEntity.badRequest().body(response);
    }

    private ErrorDetail toErrorDetail(FieldError fieldError) {
        return ErrorDetail.field(fieldError.getField(), fieldError.getDefaultMessage());
    }
}

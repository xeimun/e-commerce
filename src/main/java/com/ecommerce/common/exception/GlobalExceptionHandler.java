package com.ecommerce.common.exception;

import com.ecommerce.cart.exception.CartItemNotFoundException;
import com.ecommerce.cart.exception.CartItemQuantityExceededException;
import com.ecommerce.coupon.exception.CouponAlreadyIssuedException;
import com.ecommerce.coupon.exception.CouponExpiredException;
import com.ecommerce.coupon.exception.CouponNotFoundException;
import com.ecommerce.coupon.exception.FirstOrderCouponNotAvailableException;
import com.ecommerce.order.exception.OrderNotPaymentPendingException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.exception.OrderPaymentExpiredException;
import com.ecommerce.order.exception.OrderValidationException;
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

    @ExceptionHandler(CouponNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCouponNotFound(CouponNotFoundException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "COUPON_NOT_FOUND",
                "쿠폰을 찾을 수 없습니다.",
                List.of(ErrorDetail.coupon(exception.getCouponId(), "COUPON_NOT_FOUND"))
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(CouponAlreadyIssuedException.class)
    public ResponseEntity<ApiErrorResponse> handleCouponAlreadyIssued(CouponAlreadyIssuedException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "COUPON_ALREADY_ISSUED",
                "이미 발급받은 쿠폰입니다.",
                List.of(ErrorDetail.coupon(exception.getCouponId(), "COUPON_ALREADY_ISSUED"))
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(CouponExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleCouponExpired(CouponExpiredException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "COUPON_EXPIRED",
                "만료된 쿠폰입니다.",
                List.of(ErrorDetail.coupon(exception.getCouponId(), "COUPON_EXPIRED"))
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(FirstOrderCouponNotAvailableException.class)
    public ResponseEntity<ApiErrorResponse> handleFirstOrderCouponNotAvailable(
            FirstOrderCouponNotAvailableException exception
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "FIRST_ORDER_COUPON_NOT_AVAILABLE",
                "첫 주문 전용 쿠폰을 발급할 수 없습니다.",
                List.of(ErrorDetail.coupon(exception.getCouponId(), "FIRST_ORDER_COUPON_NOT_AVAILABLE"))
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(OrderValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderValidation(OrderValidationException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "ORDER_VALIDATION_FAILED",
                exception.getMessage(),
                exception.getDetails()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(OrderNotFoundException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "ORDER_NOT_FOUND",
                "주문을 찾을 수 없습니다.",
                List.of(ErrorDetail.order(exception.getOrderId(), "ORDER_NOT_FOUND"))
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(OrderNotPaymentPendingException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotPaymentPending(OrderNotPaymentPendingException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "ORDER_NOT_PAYMENT_PENDING",
                "결제 대기 상태의 주문만 처리할 수 있습니다.",
                List.of(ErrorDetail.order(exception.getOrderId(), "ORDER_NOT_PAYMENT_PENDING"))
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(OrderPaymentExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderPaymentExpired(OrderPaymentExpiredException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "ORDER_PAYMENT_EXPIRED",
                "결제 대기 시간이 만료되었습니다.",
                List.of(ErrorDetail.order(exception.getOrderId(), "ORDER_PAYMENT_EXPIRED"))
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

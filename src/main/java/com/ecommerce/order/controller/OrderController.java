package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderCreateRequest;
import com.ecommerce.order.dto.OrderCreateResponse;
import com.ecommerce.order.dto.OrderDetailResponse;
import com.ecommerce.order.dto.OrderPaymentCancelResponse;
import com.ecommerce.order.dto.OrderPaymentSuccessResponse;
import com.ecommerce.order.dto.OrderSummaryResponse;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderCreateResponse createOrder(
            @RequestHeader("X-Customer-Id") Long customerId,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        return orderService.createOrder(customerId, request);
    }

    @GetMapping
    public List<OrderSummaryResponse> getOrders(@RequestHeader("X-Customer-Id") Long customerId) {
        return orderService.getOrders(customerId);
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long orderId
    ) {
        return orderService.getOrder(customerId, orderId);
    }

    @PostMapping("/{orderId}/payment/success")
    public OrderPaymentSuccessResponse completePayment(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long orderId
    ) {
        return orderService.completePayment(customerId, orderId);
    }

    @PostMapping("/{orderId}/payment/cancel")
    public OrderPaymentCancelResponse cancelPayment(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long orderId
    ) {
        return orderService.cancelPayment(customerId, orderId);
    }
}

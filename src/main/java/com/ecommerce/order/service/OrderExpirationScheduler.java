package com.ecommerce.order.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.order.payment-expiration",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OrderExpirationScheduler {

    private final OrderService orderService;

    public OrderExpirationScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(
            initialDelayString = "${app.order.payment-expiration.initial-delay-ms:60000}",
            fixedDelayString = "${app.order.payment-expiration.fixed-delay-ms:60000}"
    )
    public void expirePaymentPendingOrders() {
        orderService.expirePaymentPendingOrders();
    }
}

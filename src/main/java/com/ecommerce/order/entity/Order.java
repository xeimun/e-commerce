package com.ecommerce.order.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 30)
    private OrderCancelReason cancelReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_canceled_at")
    private LocalDateTime paymentCanceledAt;

    @Column(name = "total_product_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalProductAmount;

    @Column(name = "total_instant_discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalInstantDiscountAmount;

    @Column(name = "total_coupon_discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCouponDiscountAmount;

    @Column(name = "final_payment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal finalPaymentAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Order() {
    }

    private Order(Long customerId, LocalDateTime expiresAt) {
        this.customerId = requirePositiveCustomerId(customerId);
        this.status = OrderStatus.PAYMENT_PENDING;
        this.expiresAt = Objects.requireNonNull(expiresAt, "결제 대기 만료 시간은 필수입니다.");
        this.totalProductAmount = BigDecimal.ZERO;
        this.totalInstantDiscountAmount = BigDecimal.ZERO;
        this.totalCouponDiscountAmount = BigDecimal.ZERO;
        this.finalPaymentAmount = BigDecimal.ZERO;
    }

    public static Order create(Long customerId, LocalDateTime expiresAt, List<OrderItem> orderItems) {
        return create(customerId, expiresAt, orderItems, BigDecimal.ZERO);
    }

    public static Order create(
            Long customerId,
            LocalDateTime expiresAt,
            List<OrderItem> orderItems,
            BigDecimal orderCouponDiscountAmount
    ) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("주문 상품은 1개 이상이어야 합니다.");
        }

        Order order = new Order(customerId, expiresAt);
        orderItems.forEach(order::addItem);
        order.recalculateAmounts(orderCouponDiscountAmount);

        return order;
    }

    private void addItem(OrderItem orderItem) {
        OrderItem item = Objects.requireNonNull(orderItem, "주문 상품은 필수입니다.");
        item.assignOrder(this);
        this.items.add(item);
    }

    private void recalculateAmounts(BigDecimal orderCouponDiscountAmount) {
        this.totalProductAmount = items.stream()
                .map(OrderItem::getOriginalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalInstantDiscountAmount = items.stream()
                .map(OrderItem::getInstantDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProductCouponDiscountAmount = items.stream()
                .map(OrderItem::getProductCouponDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paymentAmountBeforeOrderCoupon = items.stream()
                .map(OrderItem::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal orderCouponDiscount = clampOrderCouponDiscount(orderCouponDiscountAmount, paymentAmountBeforeOrderCoupon);
        this.totalCouponDiscountAmount = totalProductCouponDiscountAmount.add(orderCouponDiscount);
        this.finalPaymentAmount = paymentAmountBeforeOrderCoupon
                .subtract(orderCouponDiscount)
                .max(BigDecimal.ZERO);
    }

    public void completePayment(LocalDateTime paidAt) {
        requirePaymentPending();
        this.status = OrderStatus.COMPLETED;
        this.paidAt = Objects.requireNonNull(paidAt, "결제 성공 시간은 필수입니다.");
        this.cancelReason = null;
        this.paymentCanceledAt = null;
    }

    public void cancelPayment(OrderCancelReason cancelReason, LocalDateTime canceledAt) {
        requirePaymentPending();
        this.status = OrderStatus.CANCELED;
        this.cancelReason = Objects.requireNonNull(cancelReason, "주문 취소 사유는 필수입니다.");
        this.paymentCanceledAt = Objects.requireNonNull(canceledAt, "결제 취소 시간은 필수입니다.");
    }

    public boolean isPaymentPending() {
        return status == OrderStatus.PAYMENT_PENDING;
    }

    public boolean isPaymentExpired(LocalDateTime now) {
        return Objects.requireNonNull(now, "현재 시간은 필수입니다.").isAfter(expiresAt);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public OrderCancelReason getCancelReason() {
        return cancelReason;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getPaymentCanceledAt() {
        return paymentCanceledAt;
    }

    public BigDecimal getTotalProductAmount() {
        return totalProductAmount;
    }

    public BigDecimal getTotalInstantDiscountAmount() {
        return totalInstantDiscountAmount;
    }

    public BigDecimal getTotalCouponDiscountAmount() {
        return totalCouponDiscountAmount;
    }

    public BigDecimal getFinalPaymentAmount() {
        return finalPaymentAmount;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    private static Long requirePositiveCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("고객 ID는 1 이상이어야 합니다.");
        }

        return customerId;
    }

    private static BigDecimal clampOrderCouponDiscount(BigDecimal orderCouponDiscountAmount, BigDecimal maxDiscountAmount) {
        if (orderCouponDiscountAmount == null || orderCouponDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("전체 상품 쿠폰 할인 금액은 0 이상이어야 합니다.");
        }

        if (orderCouponDiscountAmount.compareTo(maxDiscountAmount) > 0) {
            return maxDiscountAmount;
        }

        return orderCouponDiscountAmount;
    }

    private void requirePaymentPending() {
        if (!isPaymentPending()) {
            throw new IllegalStateException("결제 대기 상태의 주문만 처리할 수 있습니다.");
        }
    }
}

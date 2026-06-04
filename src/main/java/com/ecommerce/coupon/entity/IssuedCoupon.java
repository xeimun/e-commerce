package com.ecommerce.coupon.entity;

import com.ecommerce.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "issued_coupons",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_issued_coupons_coupon_customer",
                columnNames = {"coupon_id", "customer_id"}
        )
)
public class IssuedCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssuedCouponStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected IssuedCoupon() {
    }

    private IssuedCoupon(Coupon coupon, Long customerId, LocalDateTime issuedAt) {
        this.coupon = Objects.requireNonNull(coupon, "쿠폰은 필수입니다.");
        this.customerId = requirePositiveCustomerId(customerId);
        this.status = IssuedCouponStatus.AVAILABLE;
        this.issuedAt = Objects.requireNonNull(issuedAt, "쿠폰 발급 시간은 필수입니다.");
    }

    public static IssuedCoupon issue(Coupon coupon, Long customerId, LocalDateTime issuedAt) {
        return new IssuedCoupon(coupon, customerId, issuedAt);
    }

    public void reserve(Order order, LocalDateTime reservedAt) {
        requireStatus(IssuedCouponStatus.AVAILABLE);
        this.order = Objects.requireNonNull(order, "쿠폰 예약 주문은 필수입니다.");
        this.status = IssuedCouponStatus.RESERVED;
        this.reservedAt = Objects.requireNonNull(reservedAt, "쿠폰 예약 시간은 필수입니다.");
        this.usedAt = null;
    }

    public void use(LocalDateTime usedAt) {
        requireStatus(IssuedCouponStatus.RESERVED);
        this.status = IssuedCouponStatus.USED;
        this.usedAt = Objects.requireNonNull(usedAt, "쿠폰 사용 시간은 필수입니다.");
    }

    public void releaseReservation() {
        requireStatus(IssuedCouponStatus.RESERVED);
        this.status = IssuedCouponStatus.AVAILABLE;
        this.order = null;
        this.reservedAt = null;
        this.usedAt = null;
    }

    public boolean isOwnedBy(Long customerId) {
        return Objects.equals(this.customerId, customerId);
    }

    public boolean isAvailableAt(LocalDateTime now) {
        return status == IssuedCouponStatus.AVAILABLE && !coupon.isExpired(now);
    }

    public String unavailableReason(LocalDateTime now) {
        if (status == IssuedCouponStatus.USED) {
            return "COUPON_ALREADY_USED";
        }
        if (status == IssuedCouponStatus.RESERVED) {
            return "COUPON_RESERVED";
        }
        if (status == IssuedCouponStatus.EXPIRED || coupon.isExpired(now)) {
            return "COUPON_EXPIRED";
        }

        return "COUPON_NOT_AVAILABLE";
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

    public Coupon getCoupon() {
        return coupon;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Order getOrder() {
        return order;
    }

    public IssuedCouponStatus getStatus() {
        return status;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    private static Long requirePositiveCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("고객 ID는 1 이상이어야 합니다.");
        }

        return customerId;
    }

    private void requireStatus(IssuedCouponStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException(expectedStatus + " 상태의 쿠폰만 처리할 수 있습니다.");
        }
    }
}

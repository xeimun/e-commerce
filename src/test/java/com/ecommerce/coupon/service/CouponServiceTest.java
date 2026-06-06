package com.ecommerce.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.coupon.dto.IssuableCouponResponse;
import com.ecommerce.coupon.dto.IssuedCouponIssueResponse;
import com.ecommerce.coupon.dto.MyCouponResponse;
import com.ecommerce.coupon.entity.Coupon;
import com.ecommerce.coupon.entity.CouponStatus;
import com.ecommerce.coupon.entity.IssuedCoupon;
import com.ecommerce.coupon.entity.IssuedCouponStatus;
import com.ecommerce.coupon.exception.CouponAlreadyIssuedException;
import com.ecommerce.coupon.exception.CouponExpiredException;
import com.ecommerce.coupon.exception.CouponNotFoundException;
import com.ecommerce.coupon.exception.CouponNotIssuableException;
import com.ecommerce.coupon.repository.CouponRepository;
import com.ecommerce.coupon.repository.IssuedCouponRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    private CouponService couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponService(couponRepository, issuedCouponRepository);
    }

    @Test
    void getIssuableCouponsMarksAlreadyIssuedCouponAsNotIssuable() {
        Coupon firstCoupon = orderCouponFixture(1L, "전체 상품 3000원 할인", LocalDateTime.now().plusDays(1));
        Coupon secondCoupon = orderCouponFixture(2L, "전체 상품 5000원 할인", LocalDateTime.now().plusDays(1));
        when(couponRepository.findAllByStatusAndExpiresAtGreaterThanEqualOrderByIdAsc(
                eq(CouponStatus.ACTIVE),
                any(LocalDateTime.class)
        ))
                .thenReturn(List.of(firstCoupon, secondCoupon));
        when(issuedCouponRepository.findIssuedCouponIdsByCustomerIdAndCouponIdIn(eq(7L), any()))
                .thenReturn(List.of(2L));

        List<IssuableCouponResponse> response = couponService.getIssuableCoupons(7L);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).couponId()).isEqualTo(1L);
        assertThat(response.get(0).issuable()).isTrue();
        assertThat(response.get(1).couponId()).isEqualTo(2L);
        assertThat(response.get(1).issuable()).isFalse();
    }

    @Test
    void issueCouponCreatesAvailableIssuedCoupon() {
        Coupon coupon = orderCouponFixture(1L, "전체 상품 3000원 할인", LocalDateTime.now().plusDays(1));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(issuedCouponRepository.existsByCouponIdAndCustomerId(1L, 7L)).thenReturn(false);
        when(issuedCouponRepository.saveAndFlush(any(IssuedCoupon.class))).thenAnswer(invocation -> {
            IssuedCoupon issuedCoupon = invocation.getArgument(0);
            ReflectionTestUtils.setField(issuedCoupon, "id", 10L);
            return issuedCoupon;
        });

        IssuedCouponIssueResponse response = couponService.issueCoupon(7L, 1L);

        assertThat(response.issuedCouponId()).isEqualTo(10L);
        assertThat(response.couponId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(IssuedCouponStatus.AVAILABLE);
        assertThat(response.issuedAt()).isNotNull();
        verify(issuedCouponRepository).saveAndFlush(any(IssuedCoupon.class));
    }

    @Test
    void issueCouponThrowsExceptionWhenCouponDoesNotExist() {
        when(couponRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issueCoupon(7L, 99L))
                .isInstanceOf(CouponNotFoundException.class);
        verify(issuedCouponRepository, never()).saveAndFlush(any());
    }

    @Test
    void issueCouponThrowsExceptionWhenCouponIsExpired() {
        Coupon coupon = orderCouponFixture(1L, "전체 상품 3000원 할인", LocalDateTime.now().minusMinutes(1));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.issueCoupon(7L, 1L))
                .isInstanceOf(CouponExpiredException.class);
        verify(issuedCouponRepository, never()).saveAndFlush(any());
    }

    @Test
    void issueCouponThrowsExceptionWhenCouponIsStopped() {
        Coupon coupon = orderCouponFixture(1L, "전체 상품 3000원 할인", LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(coupon, "status", CouponStatus.STOPPED);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.issueCoupon(7L, 1L))
                .isInstanceOf(CouponNotIssuableException.class);
        verify(issuedCouponRepository, never()).existsByCouponIdAndCustomerId(any(), any());
        verify(issuedCouponRepository, never()).saveAndFlush(any());
    }

    @Test
    void issueCouponThrowsExceptionWhenCustomerAlreadyIssuedSameCoupon() {
        Coupon coupon = orderCouponFixture(1L, "전체 상품 3000원 할인", LocalDateTime.now().plusDays(1));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(issuedCouponRepository.existsByCouponIdAndCustomerId(1L, 7L)).thenReturn(true);

        assertThatThrownBy(() -> couponService.issueCoupon(7L, 1L))
                .isInstanceOf(CouponAlreadyIssuedException.class);
        verify(issuedCouponRepository, never()).saveAndFlush(any());
    }

    @Test
    void getMyCouponsShowsExpiredStatusForAvailableExpiredCoupon() {
        IssuedCoupon issuedCoupon = issuedCouponFixture(
                10L,
                orderCouponFixture(1L, "전체 상품 3000원 할인", LocalDateTime.now().minusMinutes(1))
        );
        when(issuedCouponRepository.findAllByCustomerIdOrderByIdDesc(7L)).thenReturn(List.of(issuedCoupon));

        List<MyCouponResponse> response = couponService.getMyCoupons(7L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).status()).isEqualTo(IssuedCouponStatus.EXPIRED);
    }

    @Test
    void getMyCouponsKeepsReservedStatusEvenWhenCouponExpiresAfterReservation() {
        IssuedCoupon issuedCoupon = issuedCouponFixture(
                10L,
                orderCouponFixture(1L, "전체 상품 3000원 할인", LocalDateTime.now().minusMinutes(1))
        );
        ReflectionTestUtils.setField(issuedCoupon, "status", IssuedCouponStatus.RESERVED);
        when(issuedCouponRepository.findAllByCustomerIdOrderByIdDesc(7L)).thenReturn(List.of(issuedCoupon));

        List<MyCouponResponse> response = couponService.getMyCoupons(7L);

        assertThat(response.get(0).status()).isEqualTo(IssuedCouponStatus.RESERVED);
    }

    private Coupon orderCouponFixture(Long couponId, String name, LocalDateTime expiresAt) {
        Coupon coupon = Coupon.createOrderCoupon(name, new BigDecimal("3000.00"), expiresAt);
        ReflectionTestUtils.setField(coupon, "id", couponId);

        return coupon;
    }

    private IssuedCoupon issuedCouponFixture(Long issuedCouponId, Coupon coupon) {
        IssuedCoupon issuedCoupon = IssuedCoupon.issue(coupon, 7L, LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(issuedCoupon, "id", issuedCouponId);

        return issuedCoupon;
    }
}

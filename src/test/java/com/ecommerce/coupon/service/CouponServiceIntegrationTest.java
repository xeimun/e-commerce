package com.ecommerce.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.coupon.dto.IssuableCouponResponse;
import com.ecommerce.coupon.dto.IssuedCouponIssueResponse;
import com.ecommerce.coupon.entity.Coupon;
import com.ecommerce.coupon.exception.CouponAlreadyIssuedException;
import com.ecommerce.coupon.repository.CouponRepository;
import com.ecommerce.coupon.repository.IssuedCouponRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CouponServiceIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @BeforeEach
    void setUp() {
        issuedCouponRepository.deleteAll();
        couponRepository.deleteAll();
    }

    @Test
    void issueCouponPersistsIssuedCouponAndPreventsDuplicateIssue() {
        Coupon coupon = couponRepository.save(Coupon.createOrderCoupon(
                "전체 상품 3000원 할인",
                new BigDecimal("3000.00"),
                LocalDateTime.now().plusDays(1)
        ));

        IssuedCouponIssueResponse response = couponService.issueCoupon(7L, coupon.getId());

        assertThat(response.couponId()).isEqualTo(coupon.getId());
        assertThat(issuedCouponRepository.findAllByCustomerIdOrderByIdDesc(7L)).hasSize(1);
        assertThatThrownBy(() -> couponService.issueCoupon(7L, coupon.getId()))
                .isInstanceOf(CouponAlreadyIssuedException.class);
        assertThat(issuedCouponRepository.findAllByCustomerIdOrderByIdDesc(7L)).hasSize(1);
    }

    @Test
    void getIssuableCouponsMarksCustomerIssuedCouponAsNotIssuable() {
        Coupon firstCoupon = couponRepository.save(Coupon.createOrderCoupon(
                "전체 상품 3000원 할인",
                new BigDecimal("3000.00"),
                LocalDateTime.now().plusDays(1)
        ));
        Coupon secondCoupon = couponRepository.save(Coupon.createOrderCoupon(
                "전체 상품 5000원 할인",
                new BigDecimal("5000.00"),
                LocalDateTime.now().plusDays(1)
        ));
        couponService.issueCoupon(7L, secondCoupon.getId());

        List<IssuableCouponResponse> response = couponService.getIssuableCoupons(7L);

        assertThat(response).extracting(IssuableCouponResponse::couponId)
                .containsExactly(firstCoupon.getId(), secondCoupon.getId());
        assertThat(response.get(0).issuable()).isTrue();
        assertThat(response.get(1).issuable()).isFalse();
    }
}

package com.ecommerce.coupon.controller;

import com.ecommerce.coupon.dto.IssuableCouponResponse;
import com.ecommerce.coupon.dto.IssuedCouponIssueResponse;
import com.ecommerce.coupon.dto.MyCouponResponse;
import com.ecommerce.coupon.service.CouponService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/coupons/issuable")
    public List<IssuableCouponResponse> getIssuableCoupons(@RequestHeader("X-Customer-Id") Long customerId) {
        return couponService.getIssuableCoupons(customerId);
    }

    @PostMapping("/coupons/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public IssuedCouponIssueResponse issueCoupon(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long couponId
    ) {
        return couponService.issueCoupon(customerId, couponId);
    }

    @GetMapping("/customers/me/coupons")
    public List<MyCouponResponse> getMyCoupons(@RequestHeader("X-Customer-Id") Long customerId) {
        return couponService.getMyCoupons(customerId);
    }
}

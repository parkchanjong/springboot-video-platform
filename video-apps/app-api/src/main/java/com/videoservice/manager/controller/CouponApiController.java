package com.videoservice.manager.controller;

import com.videoservice.manager.CouponUseCase;
import com.videoservice.manager.user.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponApiController {
    private final CouponUseCase couponUseCase;

    public CouponApiController(CouponUseCase couponUseCase) {
        this.couponUseCase = couponUseCase;
    }

    @PostMapping
    void issueCoupon(
            User user,
            @RequestParam String couponPolicyId
    ) {
        couponUseCase.issueCoupon(user, couponPolicyId);
    }
}

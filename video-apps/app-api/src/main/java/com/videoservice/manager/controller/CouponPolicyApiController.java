package com.videoservice.manager.controller;

import com.videoservice.manager.CouponPolicyUseCase;
import com.videoservice.manager.coupon.CouponPolicy;
import com.videoservice.manager.dto.CouponPolicyRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupon-policies")
public class CouponPolicyApiController {
    private final CouponPolicyUseCase couponPolicyUseCase;

    public CouponPolicyApiController(CouponPolicyUseCase couponPolicyUseCase) {
        this.couponPolicyUseCase = couponPolicyUseCase;
    }

    @PostMapping
    public void createCouponPolicy(@RequestBody CouponPolicyRequest couponPolicyRequest) {
        couponPolicyUseCase.createCouponPolicy(couponPolicyRequest.toCommand());
    }

    @GetMapping("/{couponPolicyId}")
    public CouponPolicy getCouponPolicy(@PathVariable String couponPolicyId) {
        return couponPolicyUseCase.loadCouponPolicy(couponPolicyId);
    }
}

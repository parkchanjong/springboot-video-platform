package com.videoservice.manager.domain.coupon;

import com.videoservice.manager.coupon.Coupon;

public class CouponFixtures {

    public static Coupon stub(String id) {
        return Coupon.builder()
                .id(id)
                .userId("user-1")
                .couponPolicy(CouponPolicyFixtures.stub("policy-1"))
                .build();
    }
}

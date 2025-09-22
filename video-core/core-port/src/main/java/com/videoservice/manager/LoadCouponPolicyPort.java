package com.videoservice.manager;

import com.videoservice.manager.coupon.CouponPolicy;

public interface LoadCouponPolicyPort {

    CouponPolicy loadCouponPolicy(String couponPolicyId);
    long countByCouponPolicyId(String couponPolicyId);
}

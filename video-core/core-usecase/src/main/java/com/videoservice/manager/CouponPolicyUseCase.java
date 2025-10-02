package com.videoservice.manager;

import com.videoservice.manager.command.CouponPolicyCommand;
import com.videoservice.manager.coupon.CouponPolicy;

public interface CouponPolicyUseCase {

    void createCouponPolicy(CouponPolicyCommand command);
    CouponPolicy loadCouponPolicy(String couponPolicyId);
}

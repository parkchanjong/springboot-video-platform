package com.videoservice.manager;

import com.videoservice.manager.command.CouponPolicyCommand;
import com.videoservice.manager.coupon.CouponPolicy;
import java.util.Optional;

public interface CouponPolicyUseCase {

    void createCouponPolicy(CouponPolicyCommand command);
    Optional<CouponPolicy> loadCouponPolicy(String couponPolicyId);
}

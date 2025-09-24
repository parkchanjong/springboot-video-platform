package com.videoservice.manager;

import com.videoservice.manager.coupon.CouponPolicy;
import java.util.Optional;

public interface LoadCouponPolicyPort {

    Optional<CouponPolicy> loadCouponPolicy(String couponPolicyId);
}

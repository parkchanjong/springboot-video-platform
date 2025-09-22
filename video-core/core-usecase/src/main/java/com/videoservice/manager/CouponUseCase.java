package com.videoservice.manager;

import com.videoservice.manager.user.User;

public interface CouponUseCase {

    void issueCoupon(User user, String couponPolicyId);
}

package com.videoservice.manager;

import com.videoservice.manager.coupon.Coupon;

public interface CouponPort {

    void saveCoupon(Coupon coupon);

    void updateCouponState(Coupon coupon);

    Coupon getCouponState(String couponId);
}

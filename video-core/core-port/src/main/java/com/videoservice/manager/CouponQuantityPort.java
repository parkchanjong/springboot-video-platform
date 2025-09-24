package com.videoservice.manager;

public interface CouponQuantityPort {

    long decrementQuantity(String couponPolicyId);

    void incrementQuantity(String couponPolicyId);
}

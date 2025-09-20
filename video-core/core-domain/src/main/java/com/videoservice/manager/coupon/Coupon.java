package com.videoservice.manager.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class Coupon {
    private String id;
    private String userId;
    private CouponPolicy couponPolicy;
}

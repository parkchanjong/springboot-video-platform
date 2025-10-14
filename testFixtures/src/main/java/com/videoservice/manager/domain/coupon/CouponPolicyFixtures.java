package com.videoservice.manager.domain.coupon;

import com.videoservice.manager.coupon.CouponPolicy;
import java.time.LocalDateTime;

public class CouponPolicyFixtures {
    public static CouponPolicy stub(String id) {
        return CouponPolicy.builder()
                .id(id)
                .totalQuantity(1)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .build();
    }
}

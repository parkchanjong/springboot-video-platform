package com.videoservice.manager.coupon;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class CouponPolicy {
    private String id;
    private Integer totalQuantity;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

package com.videoservice.manager.command;

import java.time.LocalDateTime;

public record CouponPolicyCommand(
        Integer totalQuantity,
        LocalDateTime startTime,
        LocalDateTime endTime
) {

}

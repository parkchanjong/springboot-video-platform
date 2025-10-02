package com.videoservice.manager.dto;

import com.videoservice.manager.command.CouponPolicyCommand;
import java.time.LocalDateTime;

public record CouponPolicyRequest(
        Integer totalQuantity,
        LocalDateTime startTime,
        LocalDateTime endTime
) {

    public CouponPolicyCommand toCommand() {
        return new CouponPolicyCommand(totalQuantity, startTime, endTime);
    }
}

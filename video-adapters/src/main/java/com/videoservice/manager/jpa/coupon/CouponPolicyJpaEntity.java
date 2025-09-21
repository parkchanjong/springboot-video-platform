package com.videoservice.manager.jpa.coupon;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "coupon_policy")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CouponPolicyJpaEntity {

    @Id
    private String id;
    private Integer totalQuantity;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

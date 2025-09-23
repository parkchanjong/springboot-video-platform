package com.videoservice.manager.jpa.coupon;

import com.videoservice.manager.channel.Channel;
import com.videoservice.manager.coupon.CouponPolicy;
import com.videoservice.manager.jpa.channel.ChannelJpaEntity;
import com.videoservice.manager.jpa.channel.ChannelSnippetJpaEntity;
import com.videoservice.manager.jpa.channel.ChannelStatisticsJpaEntity;
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

    public CouponPolicy toDomain() {
        return CouponPolicy.builder()
                .id(this.getId())
                .totalQuantity(this.getTotalQuantity())
                .startTime(this.getStartTime())
                .endTime(this.getEndTime())
                .build();
    }

    public static CouponPolicyJpaEntity from(CouponPolicy couponPolicy) {
        return new CouponPolicyJpaEntity(
                couponPolicy.getId(),
                couponPolicy.getTotalQuantity(),
                couponPolicy.getStartTime(),
                couponPolicy.getEndTime()
        );
    }
}

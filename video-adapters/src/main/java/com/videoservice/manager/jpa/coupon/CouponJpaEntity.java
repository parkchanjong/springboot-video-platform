package com.videoservice.manager.jpa.coupon;

import com.videoservice.manager.coupon.Coupon;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "coupon")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CouponJpaEntity {

    @Id
    private String id;
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_policy_id")
    private CouponPolicyJpaEntity couponPolicy;

    public Coupon toDomain() {
        return Coupon.builder()
                .id(this.getId())
                .userId(this.getUserId())
                .couponPolicy(this.getCouponPolicy().toDomain())
                .build();
    }

    public static CouponJpaEntity from(Coupon coupon) {
        return new CouponJpaEntity(
                coupon.getId(),
                coupon.getUserId(),
                CouponPolicyJpaEntity.from(coupon.getCouponPolicy())
        );
    }
}

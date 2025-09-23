package com.videoservice.manager;

import com.videoservice.manager.coupon.Coupon;
import com.videoservice.manager.exception.CouponIssueException;
import com.videoservice.manager.user.User;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class CouponService implements CouponUseCase {

    private final CouponPort couponPort;
    private final LoadCouponPolicyPort loadCouponPolicyPort;

    public CouponService(CouponPort couponPort, LoadCouponPolicyPort loadCouponPolicyPort) {
        this.couponPort = couponPort;
        this.loadCouponPolicyPort = loadCouponPolicyPort;
    }

    @Override
    public void issueCoupon(User user, String couponPolicyId) {
        var couponPolicy = loadCouponPolicyPort.loadCouponPolicy(couponPolicyId).get();

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
            throw new CouponIssueException("쿠폰 발급 기간이 아닙니다.");
        }

        long issuedCouponCount = loadCouponPolicyPort.countByCouponPolicyId(couponPolicy.getId());
        if (issuedCouponCount >= couponPolicy.getTotalQuantity()) {
            throw new CouponIssueException("쿠폰이 모두 소진되었습니다.");
        }

        var coupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .userId(user.getId())
                .build();

        couponPort.saveCoupon(coupon);
    }
}

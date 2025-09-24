package com.videoservice.manager;

import com.videoservice.manager.coupon.Coupon;
import com.videoservice.manager.exception.CouponIssueException;
import com.videoservice.manager.user.User;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class CouponService implements CouponUseCase {

    private static final long LOCK_WAIT_TIME_SECONDS = 3L;
    private static final long LOCK_LEASE_TIME_SECONDS = 10L;

    private final CouponPort couponPort;
    private final LoadCouponPolicyPort loadCouponPolicyPort;
    private final CouponQuantityPort couponQuantityPort;
    private final DistributedLockPort distributedLockPort;

    public CouponService(CouponPort couponPort,
            LoadCouponPolicyPort loadCouponPolicyPort,
            CouponQuantityPort couponQuantityPort,
            DistributedLockPort distributedLockPort) {
        this.couponPort = couponPort;
        this.loadCouponPolicyPort = loadCouponPolicyPort;
        this.couponQuantityPort = couponQuantityPort;
        this.distributedLockPort = distributedLockPort;
    }

    @Override
    public void issueCoupon(User user, String couponPolicyId) {
        var lock = distributedLockPort.getLock(couponPolicyId);

        try {
            boolean locked = lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new CouponIssueException("쿠폰 발급 요청이 많아 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
            }

            var couponPolicy = loadCouponPolicyPort.loadCouponPolicy(couponPolicyId)
                    .orElseThrow(() -> new CouponIssueException("쿠폰 정책을 찾을 수 없습니다."));

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
                throw new CouponIssueException("쿠폰 발급 기간이 아닙니다.");
            }

            long remainingQuantity = couponQuantityPort.decrementQuantity(couponPolicy.getId());
            if (remainingQuantity < 0) {
                couponQuantityPort.incrementQuantity(couponPolicy.getId());
                throw new CouponIssueException("쿠폰이 모두 소진되었습니다.");
            }

            var coupon = Coupon.builder()
                    .couponPolicy(couponPolicy)
                    .userId(user.getId())
                    .build();

            couponPort.saveCoupon(coupon);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CouponIssueException("쿠폰 발급 중 오류가 발생했습니다.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}

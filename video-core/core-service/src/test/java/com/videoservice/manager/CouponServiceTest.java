package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.videoservice.manager.coupon.Coupon;
import com.videoservice.manager.coupon.CouponPolicy;
import com.videoservice.manager.user.User;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CouponServiceTest {

    private CouponService sut;

    private final CouponPort couponPort = mock(CouponPort.class);
    private final LoadCouponPolicyPort loadCouponPolicyPort = mock(LoadCouponPolicyPort.class);
    private final CouponQuantityPort couponQuantityPort = mock(CouponQuantityPort.class);
    private final DistributedLockPort distributedLockPort = mock(DistributedLockPort.class);

    @BeforeEach
    void setUp() {
        sut = new CouponService(couponPort, loadCouponPolicyPort, couponQuantityPort, distributedLockPort);
    }

    @Test
    @DisplayName("issueCoupon")
    void testIssueCoupon() throws InterruptedException {
        // Given
        var user = User.builder()
                .id("user-1")
                .name("Tester")
                .profileImageUrl("profile.png")
                .build();
        var couponPolicyId = "coupon-policy-1";
        var lock = mock(DistributedLockPort.DistributedLock.class);
        var now = LocalDateTime.now();
        var couponPolicy = CouponPolicy.builder()
                .id(couponPolicyId)
                .totalQuantity(10)
                .startTime(now.minusMinutes(1))
                .endTime(now.plusMinutes(1))
                .build();

        given(distributedLockPort.getLock(couponPolicyId)).willReturn(lock);
        given(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.SECONDS))).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);
        given(loadCouponPolicyPort.loadCouponPolicy(couponPolicyId)).willReturn(Optional.of(couponPolicy));
        given(couponQuantityPort.decrementQuantity(couponPolicyId)).willReturn(5L);

        // When
        sut.issueCoupon(user, couponPolicyId);

        // Then
        var couponCaptor = ArgumentCaptor.forClass(Coupon.class);
        verify(distributedLockPort).getLock(couponPolicyId);
        verify(lock).tryLock(3L, 10L, TimeUnit.SECONDS);
        verify(loadCouponPolicyPort).loadCouponPolicy(couponPolicyId);
        verify(couponQuantityPort).decrementQuantity(couponPolicyId);
        verify(couponPort).saveCoupon(couponCaptor.capture());
        verify(lock).unlock();

        var savedCoupon = couponCaptor.getValue();
        then(savedCoupon.getUserId()).isEqualTo(user.getId());
        then(savedCoupon.getCouponPolicy()).isEqualTo(couponPolicy);
    }
}

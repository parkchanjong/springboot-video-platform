package com.videoservice.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoservice.manager.coupon.Coupon;
import com.videoservice.manager.exception.CouponIssueException;
import com.videoservice.manager.jpa.coupon.CouponJpaEntity;
import com.videoservice.manager.jpa.coupon.CouponJpaRepository;
import com.videoservice.manager.redis.common.RedisKeyGenerator;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class CouponPersistenceAdapter implements CouponPort {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final CouponJpaRepository couponJpaRepository;

    public CouponPersistenceAdapter(
            RedissonClient redissonClient, ObjectMapper objectMapper, CouponJpaRepository couponJpaRepository
    ) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.couponJpaRepository = couponJpaRepository;
    }

    @Override
    public void saveCoupon(Coupon coupon) {
        couponJpaRepository.save(CouponJpaEntity.from(coupon));
    }

    @Override
    public void updateCouponState(Coupon coupon) {
        try {
            var stateKey = RedisKeyGenerator.getCouponStateKey(coupon.getId());
            var couponJson = objectMapper.writeValueAsString(stateKey);
            var bucket = redissonClient.getBucket(stateKey);
            bucket.set(couponJson);

        } catch (Exception e) {
            throw new CouponIssueException("쿠폰 상태 업데이트 중 오류가 발생했습니다.");
        }
    }

    @Override
    public Coupon getCouponState(String couponId) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(
                    RedisKeyGenerator.getCouponStateKey(couponId));
            var couponJson = bucket.get();

            if (couponJson == null) {
                return null;
            }

            return objectMapper.readValue(couponJson, Coupon.class);
        } catch (Exception e) {
            throw new CouponIssueException("쿠폰 상태 조회 중 오류가 발생했습니다.");
        }
    }
}

package com.videoservice.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoservice.manager.coupon.CouponPolicy;
import com.videoservice.manager.exception.CouponIssueException;
import com.videoservice.manager.jpa.coupon.CouponPolicyJpaEntity;
import com.videoservice.manager.jpa.coupon.CouponPolicyJpaRepository;
import com.videoservice.manager.redis.common.RedisKeyGenerator;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CouponPolicyPersistenceAdapter implements LoadCouponPolicyPort, SaveCouponPolicyPort {

    private final CouponPolicyJpaRepository couponPolicyJpaRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public CouponPolicyPersistenceAdapter(CouponPolicyJpaRepository couponPolicyJpaRepository,
            RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.couponPolicyJpaRepository = couponPolicyJpaRepository;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveCouponPolicy(CouponPolicy couponPolicy) {

        var savedPolicy = couponPolicyJpaRepository.save(CouponPolicyJpaEntity.from(couponPolicy));

        var atomicQuantity = redissonClient.getAtomicLong(RedisKeyGenerator.getCouponQuantityKeyKey(savedPolicy.getId()));
        atomicQuantity.set(savedPolicy.getTotalQuantity());

        String policyJson;
        try {
            policyJson = objectMapper.writeValueAsString(savedPolicy);
        } catch (JsonProcessingException e) {
            throw new CouponIssueException("JsonProcessingException.");
        }
        var bucket = redissonClient.getBucket(RedisKeyGenerator.getCouponPolicyKeyKey(savedPolicy.getId()));
        bucket.set(policyJson);
    }

    @Override
    public Optional<CouponPolicy> loadCouponPolicy(String couponPolicyId) {

        RBucket<String> bucket = redissonClient.getBucket(RedisKeyGenerator.getCouponPolicyKeyKey(couponPolicyId));
        String policyJson = bucket.get();
        if (policyJson != null) {
            try {
                return Optional.ofNullable(objectMapper.readValue(policyJson, CouponPolicy.class));
            } catch (JsonProcessingException e) {
                throw new CouponIssueException("쿠폰 정책 정보를 JSON으로 파싱하는 중 오류가 발생했습니다.");
            }
        }

        return couponPolicyJpaRepository.findById(couponPolicyId)
                .map(CouponPolicyJpaEntity::toDomain);
    }

    @Override
    public long countByCouponPolicyId(String couponPolicyId) {
        var atomicQuantity = redissonClient.getAtomicLong(RedisKeyGenerator.getCouponPolicyKeyKey(couponPolicyId));
        return atomicQuantity.decrementAndGet();
    }
}

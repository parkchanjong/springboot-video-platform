package com.videoservice.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoservice.manager.coupon.CouponPolicy;
import com.videoservice.manager.exception.CouponIssueException;
import com.videoservice.manager.jpa.coupon.CouponPolicyJpaEntity;
import com.videoservice.manager.jpa.coupon.CouponPolicyJpaRepository;
import com.videoservice.manager.redis.common.RedisKeyGenerator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

class CouponPolicyPersistenceAdapterTest {

    private CouponPolicyPersistenceAdapter sut;

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final CouponPolicyJpaRepository couponPolicyJpaRepository = mock(CouponPolicyJpaRepository.class);

    @BeforeEach
    void setUp() {
        reset(redissonClient, objectMapper, couponPolicyJpaRepository);
        sut = new CouponPolicyPersistenceAdapter(couponPolicyJpaRepository, redissonClient, objectMapper);
    }

    @DisplayName("Given coupon policy when saveCouponPolicy then persist entity and cache to Redis")
    @Test
    void givenCouponPolicy_whenSaveCouponPolicy_thenPersistEntityAndCacheToRedis() throws Exception {
        // given
        var couponPolicy = createCouponPolicy();
        var savedEntity = CouponPolicyJpaEntity.from(couponPolicy);
        var quantityKey = RedisKeyGenerator.getCouponQuantityKeyKey(couponPolicy.getId());
        var policyKey = RedisKeyGenerator.getCouponPolicyKeyKey(couponPolicy.getId());
        var atomicLong = mockAtomicLong();
        var bucket = mockStringBucket();
        var policyJson = "{\"id\":\"coupon-policy\"}";

        when(couponPolicyJpaRepository.save(any())).thenReturn(savedEntity);
        when(redissonClient.getAtomicLong(quantityKey)).thenReturn(atomicLong);
        when(redissonClient.<String>getBucket(policyKey)).thenReturn(bucket);
        when(objectMapper.writeValueAsString(savedEntity)).thenReturn(policyJson);

        // when
        sut.saveCouponPolicy(couponPolicy);

        // then
        var captor = ArgumentCaptor.forClass(CouponPolicyJpaEntity.class);
        verify(couponPolicyJpaRepository).save(captor.capture());
        var persistedEntity = captor.getValue();

        assertThat(persistedEntity.getId()).isEqualTo(couponPolicy.getId());
        assertThat(persistedEntity.getTotalQuantity()).isEqualTo(couponPolicy.getTotalQuantity());
        assertThat(persistedEntity.getStartTime()).isEqualTo(couponPolicy.getStartTime());
        assertThat(persistedEntity.getEndTime()).isEqualTo(couponPolicy.getEndTime());

        verify(redissonClient).getAtomicLong(quantityKey);
        verify(atomicLong).set(savedEntity.getTotalQuantity());
        verify(redissonClient).getBucket(policyKey);
        verify(bucket).set(policyJson);
    }

    @DisplayName("Given serialization failure when saveCouponPolicy then throw CouponIssueException")
    @Test
    void givenSerializationFailure_whenSaveCouponPolicy_thenThrowCouponIssueException() throws Exception {
        // given
        var couponPolicy = createCouponPolicy();
        var savedEntity = CouponPolicyJpaEntity.from(couponPolicy);
        var quantityKey = RedisKeyGenerator.getCouponQuantityKeyKey(couponPolicy.getId());
        var policyKey = RedisKeyGenerator.getCouponPolicyKeyKey(couponPolicy.getId());
        var atomicLong = mockAtomicLong();
        when(couponPolicyJpaRepository.save(any())).thenReturn(savedEntity);
        when(redissonClient.getAtomicLong(quantityKey)).thenReturn(atomicLong);
        when(objectMapper.writeValueAsString(savedEntity)).thenThrow(new JsonProcessingException("fail") {});

        // when & then
        assertThatThrownBy(() -> sut.saveCouponPolicy(couponPolicy))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage("JsonProcessingException.");

        verify(redissonClient).getAtomicLong(quantityKey);
        verify(atomicLong).set(savedEntity.getTotalQuantity());
        verify(redissonClient, never()).getBucket(policyKey);
    }

    @DisplayName("Given policy cached in Redis when loadCouponPolicy then return deserialized policy")
    @Test
    void givenPolicyCachedInRedis_whenLoadCouponPolicy_thenReturnDeserializedPolicy() throws Exception {
        // given
        var couponPolicyId = "policy-id";
        var policyJson = "{\"id\":\"policy-id\"}";
        var couponPolicy = createCouponPolicy(couponPolicyId);
        var policyKey = RedisKeyGenerator.getCouponPolicyKeyKey(couponPolicyId);
        var bucket = mockStringBucket();

        when(redissonClient.<String>getBucket(policyKey)).thenReturn(bucket);
        when(bucket.get()).thenReturn(policyJson);
        when(objectMapper.readValue(policyJson, CouponPolicy.class)).thenReturn(couponPolicy);

        // when
        var result = sut.loadCouponPolicy(couponPolicyId);

        // then
        assertThat(result).contains(couponPolicy);
        verify(redissonClient).getBucket(policyKey);
        verify(bucket).get();
        verify(objectMapper).readValue(policyJson, CouponPolicy.class);
        verifyNoInteractions(couponPolicyJpaRepository);
    }

    @DisplayName("Given missing Redis cache when loadCouponPolicy then fallback to repository")
    @Test
    void givenMissingRedisCache_whenLoadCouponPolicy_thenFallbackToRepository()
            throws JsonProcessingException {
        // given
        var couponPolicyId = "policy-id";
        var policyKey = RedisKeyGenerator.getCouponPolicyKeyKey(couponPolicyId);
        var bucket = mockStringBucket();
        var entity = CouponPolicyJpaEntity.from(createCouponPolicy(couponPolicyId));

        when(redissonClient.<String>getBucket(policyKey)).thenReturn(bucket);
        when(bucket.get()).thenReturn(null);
        when(couponPolicyJpaRepository.findById(couponPolicyId)).thenReturn(Optional.of(entity));

        // when
        var result = sut.loadCouponPolicy(couponPolicyId);

        // then
        assertThat(result).isPresent();
        var policy = result.orElseThrow();
        assertThat(policy.getId()).isEqualTo(entity.getId());
        assertThat(policy.getTotalQuantity()).isEqualTo(entity.getTotalQuantity());
        assertThat(policy.getStartTime()).isEqualTo(entity.getStartTime());
        assertThat(policy.getEndTime()).isEqualTo(entity.getEndTime());

        verify(redissonClient).getBucket(policyKey);
        verify(bucket).get();
        verify(couponPolicyJpaRepository).findById(couponPolicyId);
        verify(objectMapper, never()).readValue(anyString(), eq(CouponPolicy.class));
    }

    @DisplayName("Given JSON parsing failure when loadCouponPolicy then throw CouponIssueException")
    @Test
    void givenJsonParsingFailure_whenLoadCouponPolicy_thenThrowCouponIssueException() throws Exception {
        // given
        var couponPolicyId = "policy-id";
        var policyKey = RedisKeyGenerator.getCouponPolicyKeyKey(couponPolicyId);
        var policyJson = "{\"id\":\"policy-id\"}";
        var bucket = mockStringBucket();

        when(redissonClient.<String>getBucket(policyKey)).thenReturn(bucket);
        when(bucket.get()).thenReturn(policyJson);
        when(objectMapper.readValue(policyJson, CouponPolicy.class)).thenThrow(new JsonProcessingException("fail") {});

        // when & then
        assertThatThrownBy(() -> sut.loadCouponPolicy(couponPolicyId))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage("쿠폰 정책 정보를 JSON으로 파싱하는 중 오류가 발생했습니다.");
        verifyNoInteractions(couponPolicyJpaRepository);
    }

    @DisplayName("Given policy id when decrementQuantity then return remaining quantity from Redis")
    @Test
    void givenPolicyId_whenDecrementQuantity_thenReturnRemainingQuantity() {
        // given
        var couponPolicyId = "policy-id";
        var quantityKey = RedisKeyGenerator.getCouponQuantityKeyKey(couponPolicyId);
        var atomicLong = mockAtomicLong();

        when(redissonClient.getAtomicLong(quantityKey)).thenReturn(atomicLong);
        when(atomicLong.decrementAndGet()).thenReturn(42L);

        // when
        var result = sut.decrementQuantity(couponPolicyId);

        // then
        assertThat(result).isEqualTo(42L);
        verify(redissonClient).getAtomicLong(quantityKey);
        verify(atomicLong).decrementAndGet();
    }

    @DisplayName("Given policy id when incrementQuantity then increment Redis counter")
    @Test
    void givenPolicyId_whenIncrementQuantity_thenIncrementRedisCounter() {
        // given
        var couponPolicyId = "policy-id";
        var quantityKey = RedisKeyGenerator.getCouponQuantityKeyKey(couponPolicyId);
        var atomicLong = mockAtomicLong();

        when(redissonClient.getAtomicLong(quantityKey)).thenReturn(atomicLong);

        // when
        sut.incrementQuantity(couponPolicyId);

        // then
        verify(redissonClient).getAtomicLong(quantityKey);
        verify(atomicLong).incrementAndGet();
    }

    private CouponPolicy createCouponPolicy() {
        return createCouponPolicy("coupon-policy-id");
    }

    private CouponPolicy createCouponPolicy(String id) {
        return CouponPolicy.builder()
                .id(id)
                .totalQuantity(100)
                .startTime(LocalDateTime.of(2024, 1, 1, 0, 0))
                .endTime(LocalDateTime.of(2024, 12, 31, 23, 59))
                .build();
    }

    @SuppressWarnings("unchecked")
    private RBucket<String> mockStringBucket() {
        return (RBucket<String>) mock(RBucket.class);
    }

    private RAtomicLong mockAtomicLong() {
        return mock(RAtomicLong.class);
    }
}

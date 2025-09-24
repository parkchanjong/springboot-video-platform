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
import com.videoservice.manager.coupon.Coupon;
import com.videoservice.manager.coupon.CouponPolicy;
import com.videoservice.manager.exception.CouponIssueException;
import com.videoservice.manager.jpa.coupon.CouponJpaEntity;
import com.videoservice.manager.jpa.coupon.CouponJpaRepository;
import com.videoservice.manager.redis.common.RedisKeyGenerator;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

class CouponPersistenceAdapterTest {

    private CouponPersistenceAdapter sut;

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final CouponJpaRepository couponJpaRepository = mock(CouponJpaRepository.class);

    @BeforeEach
    void setUp() {
        reset(redissonClient, objectMapper, couponJpaRepository);
        sut = new CouponPersistenceAdapter(redissonClient, objectMapper, couponJpaRepository);
    }

    @DisplayName("Given coupon when saveCoupon then delegate to repository")
    @Test
    void givenCoupon_whenSaveCoupon_thenDelegateToRepository() {
        // given
        var coupon = createCoupon();

        // when
        sut.saveCoupon(coupon);

        // then
        var captor = ArgumentCaptor.forClass(CouponJpaEntity.class);
        verify(couponJpaRepository).save(captor.capture());
        var savedEntity = captor.getValue();

        assertThat(savedEntity.getId()).isEqualTo(coupon.getId());
        assertThat(savedEntity.getUserId()).isEqualTo(coupon.getUserId());
        assertThat(savedEntity.getCouponPolicy().getId())
                .isEqualTo(coupon.getCouponPolicy().getId());
        assertThat(savedEntity.getCouponPolicy().getTotalQuantity())
                .isEqualTo(coupon.getCouponPolicy().getTotalQuantity());
        assertThat(savedEntity.getCouponPolicy().getStartTime())
                .isEqualTo(coupon.getCouponPolicy().getStartTime());
        assertThat(savedEntity.getCouponPolicy().getEndTime())
                .isEqualTo(coupon.getCouponPolicy().getEndTime());
    }

    @DisplayName("Given coupon when updateCouponState then write serialized state to Redis")
    @Test
    void givenCoupon_whenUpdateCouponState_thenWriteSerializedStateToRedis()
            throws JsonProcessingException {
        // given
        var coupon = createCoupon();
        var couponStateKey = RedisKeyGenerator.getCouponStateKey(coupon.getId());
        var couponJson = "coupon-json";
        var bucket = mockStringBucket();

        when(objectMapper.writeValueAsString(any())).thenReturn(couponJson);
        when(redissonClient.<String>getBucket(couponStateKey)).thenReturn(bucket);

        // when
        sut.updateCouponState(coupon);

        // then
        verify(objectMapper).writeValueAsString(any());
        verify(redissonClient).getBucket(couponStateKey);
        verify(bucket).set(couponJson);
    }

    @DisplayName("Given serialization failure when updateCouponState then throw CouponIssueException")
    @Test
    void givenSerializationFailure_whenUpdateCouponState_thenThrowException()
            throws JsonProcessingException {
        // given
        var coupon = createCoupon();
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("serialization error"));

        // when & then
        assertThatThrownBy(() -> sut.updateCouponState(coupon))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage("쿠폰 상태 업데이트 중 오류가 발생했습니다.");
        verifyNoInteractions(redissonClient);
    }

    @DisplayName("Given coupon id when getCouponState then return deserialized coupon")
    @Test
    void givenCouponId_whenGetCouponState_thenReturnDeserializedCoupon() throws Exception {
        // given
        var coupon = createCoupon();
        var couponJson = "coupon-json";
        var couponStateKey = RedisKeyGenerator.getCouponStateKey(coupon.getId());
        var bucket = mockStringBucket();

        when(redissonClient.<String>getBucket(couponStateKey)).thenReturn(bucket);
        when(bucket.get()).thenReturn(couponJson);
        when(objectMapper.readValue(couponJson, Coupon.class)).thenReturn(coupon);

        // when
        var result = sut.getCouponState(coupon.getId());

        // then
        assertThat(result).isSameAs(coupon);
        verify(bucket).get();
        verify(objectMapper).readValue(couponJson, Coupon.class);
    }

    @DisplayName("Given missing coupon state when getCouponState then return null")
    @Test
    void givenMissingCouponState_whenGetCouponState_thenReturnNull()
            throws JsonProcessingException {
        // given
        var couponId = "coupon-id";
        var couponStateKey = RedisKeyGenerator.getCouponStateKey(couponId);
        var bucket = mockStringBucket();

        when(redissonClient.<String>getBucket(couponStateKey)).thenReturn(bucket);
        when(bucket.get()).thenReturn(null);

        // when
        var result = sut.getCouponState(couponId);

        // then
        assertThat(result).isNull();
        verify(bucket).get();
        verify(objectMapper, never()).readValue(anyString(), eq(Coupon.class));
    }

    @DisplayName("Given deserialization failure when getCouponState then throw CouponIssueException")
    @Test
    void givenDeserializationFailure_whenGetCouponState_thenThrowException() throws Exception {
        // given
        var couponId = "coupon-id";
        var couponStateKey = RedisKeyGenerator.getCouponStateKey(couponId);
        var couponJson = "coupon-json";
        var bucket = mockStringBucket();

        when(redissonClient.<String>getBucket(couponStateKey)).thenReturn(bucket);
        when(bucket.get()).thenReturn(couponJson);
        when(objectMapper.readValue(couponJson, Coupon.class)).thenThrow(new RuntimeException("deserialize"));

        // when & then
        assertThatThrownBy(() -> sut.getCouponState(couponId))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage("쿠폰 상태 조회 중 오류가 발생했습니다.");
    }

    private Coupon createCoupon() {
        return Coupon.builder()
                .id("coupon-id")
                .userId("user-id")
                .couponPolicy(createCouponPolicy())
                .build();
    }

    private CouponPolicy createCouponPolicy() {
        return CouponPolicy.builder()
                .id("coupon-policy-id")
                .totalQuantity(100)
                .startTime(LocalDateTime.of(2024, 1, 1, 0, 0))
                .endTime(LocalDateTime.of(2024, 12, 31, 23, 59))
                .build();
    }

    @SuppressWarnings("unchecked")
    private RBucket<String> mockStringBucket() {
        return (RBucket<String>) mock(RBucket.class);
    }
}

package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.videoservice.manager.domain.video.VideoFixtures;
import com.videoservice.manager.jpa.VideoJpaEntityFixtures;
import com.videoservice.manager.jpa.video.VideoCustomRepository;
import com.videoservice.manager.jpa.video.VideoJpaEntity;
import com.videoservice.manager.jpa.video.VideoJpaRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class VideoPersistenceAdapterTest {
    private VideoPersistenceAdapter sut;
    private VideoCacheProperties videoCacheProperties;
    private SimpleMeterRegistry meterRegistry;

    private final VideoJpaRepository videoJpaRepository = mock(VideoJpaRepository.class);
    private final VideoCustomRepository videoCustomRepository = mock(VideoCustomRepository.class);
    private final RedisTemplate<String, Long> redisTemplate = mock(RedisTemplate.class, Mockito.RETURNS_DEEP_STUBS);
    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class, Mockito.RETURNS_DEEP_STUBS);
    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RLock lock = mock(RLock.class);
    private final ValueOperations<String, Long> valueOperations = mock(ValueOperations.class);
    private final ValueOperations<String, String> stringValueOperations = mock(ValueOperations.class);
    // 프라이빗 필드 직렬화/역직렬화를 위해 필드 가시성 설정
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    @BeforeEach
    void setUp() {
        videoCacheProperties = new VideoCacheProperties();
        videoCacheProperties.setStampedeProtectionEnabled(false);
        meterRegistry = new SimpleMeterRegistry();
        sut = new VideoPersistenceAdapter(
                videoJpaRepository,
                videoCustomRepository,
                redisTemplate,
                stringRedisTemplate,
                redissonClient,
                objectMapper,
                videoCacheProperties,
                meterRegistry
        );
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);
        lenient().when(redissonClient.getLock(anyString())).thenReturn(lock);
    }

    @Nested
    class LoadVideo {
        @Test
        void cacheHit_then_returnVideoFromCache() throws Exception {
            // Given: Redis에 캐시된 JSON이 존재
            var video = VideoFixtures.stub("video1");
            var json = objectMapper.writeValueAsString(video);
            given(stringValueOperations.get(anyString())).willReturn(json);

            // When
            var result = sut.loadVideo("video1");

            // Then: DB 호출 없이 캐시에서 반환
            then(result).extracting("id").isEqualTo("video1");
            verify(videoJpaRepository, never()).findById(any());
            then(meterRegistry.counter("video.cache.hit").count()).isEqualTo(1);
        }

        @Test
        void cacheMiss_existVideo_then_returnVideoAndCache() throws Exception {
            // Given: 캐시 미스 (기본값 null 반환), DB에 데이터 존재
            var videoJpaEntity = VideoJpaEntityFixtures.stub("video1");
            given(videoJpaRepository.findById(any())).willReturn(Optional.of(videoJpaEntity));

            // When
            var result = sut.loadVideo("video1");

            // Then: DB 조회 후 Redis에 저장
            then(result).extracting("id").isEqualTo("video1");
            verify(stringValueOperations).set("video:detail:video1", objectMapper.writeValueAsString(result), Duration.ofHours(1));
            then(meterRegistry.counter("video.cache.miss").count()).isEqualTo(1);
            then(meterRegistry.counter("video.cache.db.load").count()).isEqualTo(1);
        }

        @Test
        void cacheMiss_whenStampedeProtectionDisabled_then_eachRequestLoadsDb() {
            // Given: 개선 전 모드에서는 캐시 miss마다 DB를 조회
            var videoJpaEntity = VideoJpaEntityFixtures.stub("video1");
            given(videoJpaRepository.findById(any())).willReturn(Optional.of(videoJpaEntity));

            // When
            sut.loadVideo("video1");
            sut.loadVideo("video1");

            // Then
            verify(videoJpaRepository, times(2)).findById("video1");
            then(meterRegistry.counter("video.cache.db.load").count()).isEqualTo(2);
        }

        @Test
        void cacheMiss_notExistVideo_then_throwException() {
            // Given: 캐시 미스, DB에도 없음
            given(videoJpaRepository.findById(any())).willReturn(Optional.empty());

            // When
            var result = thenThrownBy(() -> sut.loadVideo("video1"));

            // Then
            result.isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void cacheMiss_whenLockAcquired_then_loadDbAndCache() throws Exception {
            // Given
            videoCacheProperties.setStampedeProtectionEnabled(true);
            videoCacheProperties.setDetailTtl(Duration.ofSeconds(3));
            var videoJpaEntity = VideoJpaEntityFixtures.stub("video1");
            given(lock.tryLock(100, 3, TimeUnit.MILLISECONDS)).willReturn(true);
            given(lock.isHeldByCurrentThread()).willReturn(true);
            given(videoJpaRepository.findById("video1")).willReturn(Optional.of(videoJpaEntity));

            // When
            var result = sut.loadVideo("video1");

            // Then
            then(result).extracting("id").isEqualTo("video1");
            verify(videoJpaRepository).findById("video1");
            verify(stringValueOperations).set("video:detail:video1", objectMapper.writeValueAsString(result), Duration.ofSeconds(3));
            verify(redissonClient).getLock("video:detail:lock:video1");
            verify(lock).tryLock(100, 3, TimeUnit.MILLISECONDS);
            verify(lock).isHeldByCurrentThread();
            verify(lock).unlock();
            then(meterRegistry.counter("video.cache.lock.acquired").count()).isEqualTo(1);
            then(meterRegistry.counter("video.cache.db.load").count()).isEqualTo(1);
        }

        @Test
        void cacheMiss_whenCacheFilledAfterLockAcquired_then_returnCacheWithoutDbLoad() throws Exception {
            // Given
            videoCacheProperties.setStampedeProtectionEnabled(true);
            var video = VideoFixtures.stub("video1");
            var json = objectMapper.writeValueAsString(video);
            given(stringValueOperations.get("video:detail:video1")).willReturn(null, json);
            given(lock.tryLock(100, 3, TimeUnit.MILLISECONDS)).willReturn(true);
            given(lock.isHeldByCurrentThread()).willReturn(true);

            // When
            var result = sut.loadVideo("video1");

            // Then
            then(result).extracting("id").isEqualTo("video1");
            verify(videoJpaRepository, never()).findById(anyString());
            verify(stringValueOperations, never()).set(anyString(), anyString(), any(Duration.class));
            verify(lock).unlock();
            then(meterRegistry.counter("video.cache.lock.acquired").count()).isEqualTo(1);
            then(meterRegistry.counter("video.cache.db.load").count()).isZero();
        }

        @Test
        void cacheMiss_whenLockNotAcquiredAndCacheFilledWhileWaiting_then_returnCacheWithoutDbLoad() throws Exception {
            // Given
            videoCacheProperties.setStampedeProtectionEnabled(true);
            videoCacheProperties.setLockWaitTimeout(Duration.ofMillis(20));
            videoCacheProperties.setLockRetryInterval(Duration.ofMillis(1));
            var video = VideoFixtures.stub("video1");
            var json = objectMapper.writeValueAsString(video);
            given(stringValueOperations.get("video:detail:video1")).willReturn(null, json);
            given(lock.tryLock(100, 3, TimeUnit.MILLISECONDS)).willReturn(false);

            // When
            var result = sut.loadVideo("video1");

            // Then
            then(result).extracting("id").isEqualTo("video1");
            verify(videoJpaRepository, never()).findById(anyString());
            then(meterRegistry.counter("video.cache.lock.wait").count()).isEqualTo(1);
            then(meterRegistry.counter("video.cache.lock.timeout").count()).isZero();
        }

        @Test
        void cacheMiss_whenLockWaitTimeout_then_throwExceptionWithoutDbLoad() throws Exception {
            // Given
            videoCacheProperties.setStampedeProtectionEnabled(true);
            videoCacheProperties.setLockWaitTimeout(Duration.ofMillis(2));
            videoCacheProperties.setLockRetryInterval(Duration.ofMillis(1));
            given(lock.tryLock(100, 3, TimeUnit.MILLISECONDS)).willReturn(false);

            // When
            var result = thenThrownBy(() -> sut.loadVideo("video1"));

            // Then
            result.isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("video1");
            verify(videoJpaRepository, never()).findById(anyString());
            then(meterRegistry.counter("video.cache.lock.wait").count()).isEqualTo(1);
            then(meterRegistry.counter("video.cache.lock.timeout").count()).isEqualTo(1);
        }

        @Test
        void cacheMiss_whenLockAcquiredButCurrentThreadDoesNotHoldLock_then_doesNotUnlock() throws Exception {
            // Given
            videoCacheProperties.setStampedeProtectionEnabled(true);
            var videoJpaEntity = VideoJpaEntityFixtures.stub("video1");
            given(lock.tryLock(100, 3, TimeUnit.MILLISECONDS)).willReturn(true);
            given(lock.isHeldByCurrentThread()).willReturn(false);
            given(videoJpaRepository.findById("video1")).willReturn(Optional.of(videoJpaEntity));

            // When
            sut.loadVideo("video1");

            // Then
            verify(lock).isHeldByCurrentThread();
            verify(lock, never()).unlock();
        }
    }

    @Nested
    class LoadVideoByChannel {
        @Test
        void cacheHit_then_returnVideoListFromCache() throws Exception {
            // Given: Redis에 목록 캐시가 존재
            var videos = List.of(VideoFixtures.stub("video1"), VideoFixtures.stub("video2"));
            var json = objectMapper.writeValueAsString(videos);
            given(stringValueOperations.get(anyString())).willReturn(json);

            // When
            var result = sut.loadVideoByChannel("channelId");

            // Then: DB 호출 없이 캐시에서 반환
            then(result).hasSize(2);
            verify(videoCustomRepository, never()).findByChannelId(any());
        }

        @Test
        void cacheMiss_then_returnVideoListAndCache() {
            // Given: 캐시 미스 (기본값 null 반환)
            var videoJpaEntity1 = VideoJpaEntityFixtures.stub("video1");
            var videoJpaEntity2 = VideoJpaEntityFixtures.stub("video2");
            given(videoCustomRepository.findByChannelId(any()))
                    .willReturn(List.of(videoJpaEntity1, videoJpaEntity2));

            // When
            var result = sut.loadVideoByChannel("channelId");

            // Then: DB 조회 후 Redis에 저장
            then(result).hasSize(2).extracting("id").containsExactly("video1", "video2");
            verify(stringValueOperations).set(anyString(), anyString(), any(Duration.class));
        }
    }

    @Test
    void saveVideo_then_evictVideoAndListCache() {
        // Given
        var video = VideoFixtures.stub("videoId");
        ArgumentCaptor<VideoJpaEntity> argumentCaptor = ArgumentCaptor.forClass(VideoJpaEntity.class);

        // When
        sut.saveVideo(video);

        // Then: JPA 저장 후 단건·목록 캐시 키 모두 삭제
        verify(videoJpaRepository).save(argumentCaptor.capture());
        then(argumentCaptor.getValue())
                .hasFieldOrPropertyWithValue("title", video.getTitle())
                .hasFieldOrPropertyWithValue("channelId", video.getChannelId());
        verify(stringRedisTemplate).delete("video:detail:videoId");
        verify(stringRedisTemplate).delete("video:list:channelId");
    }

    @Test
    void testIncrementViewCount() {
        given(valueOperations.increment(any())).willReturn(5L);

        sut.incrementViewCount("video1");

        verify(valueOperations).increment("video:view-count:video1");
    }

    @Test
    void testGetViewCount() {
        given(redisTemplate.opsForValue().get(any())).willReturn(10L);

        var result = sut.getViewCount("video1");

        then(result).isEqualTo(10L);
    }

    @Test
    void testGetViewCountWhenNullValueThenZero() {
        given(redisTemplate.opsForValue().get(any())).willReturn(null);

        var result = sut.getViewCount("video1");

        then(result).isEqualTo(0L);
    }

    @Test
    void testGetAllVideoIdsWithViewCount() {
        given(stringRedisTemplate.opsForSet().members(any())).willReturn(
                Set.of("videoId1", "videoId2", "videoId3"));

        var result = sut.getAllVideoIdsWithViewCount();

        then(result).contains("videoId1", "videoId2", "videoId3");
    }

    @Test
    void testSyncViewCount() {
        given(redisTemplate.opsForValue().get(any())).willReturn(20L);
        given(videoJpaRepository.findById(any())).willReturn(Optional.of(new VideoJpaEntity()));

        sut.syncViewCount("videoId");

        var argumentCaptor = ArgumentCaptor.forClass(VideoJpaEntity.class);
        verify(videoJpaRepository).save(argumentCaptor.capture());
        then(argumentCaptor.getValue().getViewCount()).isEqualTo(20L);
    }
}

package com.videoservice.manager;

import static com.videoservice.manager.redis.common.RedisKeyGenerator.getVideoKey;
import static com.videoservice.manager.redis.common.RedisKeyGenerator.getVideoListKey;
import static com.videoservice.manager.redis.common.RedisKeyGenerator.getVideoLockKey;
import static com.videoservice.manager.redis.common.RedisKeyGenerator.getVideoViewCountKey;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoservice.manager.jpa.video.VideoCustomRepository;
import com.videoservice.manager.jpa.video.VideoJpaEntity;
import com.videoservice.manager.jpa.video.VideoJpaRepository;
import com.videoservice.manager.redis.common.RedisKeyGenerator;
import com.videoservice.manager.video.Video;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VideoPersistenceAdapter implements LoadVideoPort, SaveVideoPort {
    private final VideoJpaRepository videoJpaRepository;
    private final VideoCustomRepository videoCustomRepository;
    private final RedisTemplate<String, Long> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final VideoCacheProperties videoCacheProperties;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheDbLoadCounter;
    private final Counter cacheLockAcquiredCounter;
    private final Counter cacheLockWaitCounter;
    private final Counter cacheLockTimeoutCounter;

    public VideoPersistenceAdapter(
            VideoJpaRepository videoJpaRepository,
            VideoCustomRepository videoCustomRepository,
            RedisTemplate<String, Long> redisTemplate,
            StringRedisTemplate stringRedisTemplate,
            RedissonClient redissonClient,
            ObjectMapper objectMapper,
            VideoCacheProperties videoCacheProperties,
            MeterRegistry meterRegistry
    ) {
        this.videoJpaRepository = videoJpaRepository;
        this.videoCustomRepository = videoCustomRepository;
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.videoCacheProperties = videoCacheProperties;
        this.cacheHitCounter = meterRegistry.counter("video.cache.hit");
        this.cacheMissCounter = meterRegistry.counter("video.cache.miss");
        this.cacheDbLoadCounter = meterRegistry.counter("video.cache.db.load");
        this.cacheLockAcquiredCounter = meterRegistry.counter("video.cache.lock.acquired");
        this.cacheLockWaitCounter = meterRegistry.counter("video.cache.lock.wait");
        this.cacheLockTimeoutCounter = meterRegistry.counter("video.cache.lock.timeout");
    }

    @Override
    public Video loadVideo(String videoId) {
        String key = getVideoKey(videoId);
        Video cachedVideo = getCachedVideo(key);

        if (cachedVideo != null) {
            cacheHitCounter.increment();
            return cachedVideo;
        }

        cacheMissCounter.increment();

        if (!videoCacheProperties.isStampedeProtectionEnabled()) {
            return loadVideoFromDbAndCache(videoId, key);
        }

        return loadVideoWithStampedeProtection(videoId, key);
    }

    private Video loadVideoWithStampedeProtection(String videoId, String key) {
        String lockKey = getVideoLockKey(videoId);
        RLock lock = redissonClient.getLock(lockKey);

        if (tryAcquireLock(lock, lockKey)) {
            cacheLockAcquiredCounter.increment();
            try {
                Video cachedVideo = getCachedVideo(key);
                if (cachedVideo != null) {
                    cacheHitCounter.increment();
                    return cachedVideo;
                }

                return loadVideoFromDbAndCache(videoId, key);
            } finally {
                releaseLock(lock, lockKey);
            }
        }

        cacheLockWaitCounter.increment();
        Video cachedVideo = waitForCachedVideo(key);
        if (cachedVideo != null) {
            cacheHitCounter.increment();
            return cachedVideo;
        }

        cacheLockTimeoutCounter.increment();
        throw new IllegalStateException("비디오 캐시 생성 대기 시간이 초과되었습니다. videoId=" + videoId);
    }

    private Video getCachedVideo(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }

            return objectMapper.readValue(json, Video.class);
        } catch (Exception e) {
            log.warn("캐시 조회 또는 역직렬화 실패, DB에서 조회합니다. key={}", key, e);
            return null;
        }
    }

    private Video loadVideoFromDbAndCache(String videoId, String key) {
        cacheDbLoadCounter.increment();
        Video video = videoJpaRepository.findById(videoId)
                .map(VideoJpaEntity::toDomain)
                .orElseThrow();

        cacheVideo(key, video);

        return video;
    }

    private void cacheVideo(String key, Video video) {
        try {
            stringRedisTemplate.opsForValue().set(
                    key, objectMapper.writeValueAsString(video), videoCacheProperties.getDetailTtl());
        } catch (Exception e) {
            log.warn("캐시 저장 실패. key={}", key, e);
        }
    }

    private boolean tryAcquireLock(RLock lock, String lockKey) {
        try {
            return lock.tryLock(100, 3, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cacheLockTimeoutCounter.increment();
            throw new IllegalStateException("캐시 stampede lock 획득 중 인터럽트되었습니다. lockKey=" + lockKey, e);
        }
    }

    private void releaseLock(RLock lock, String lockKey) {
        if (lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
            } catch (Exception e) {
                log.warn("캐시 stampede lock 해제 실패. lockKey={}", lockKey, e);
            }
        }
    }

    private Video waitForCachedVideo(String key) {
        long deadlineNanos = System.nanoTime() + videoCacheProperties.getLockWaitTimeout().toNanos();
        while (System.nanoTime() < deadlineNanos) {
            Video cachedVideo = getCachedVideo(key);
            if (cachedVideo != null) {
                return cachedVideo;
            }

            sleepBeforeRetry(deadlineNanos);
        }
        return null;
    }

    private void sleepBeforeRetry(long deadlineNanos) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            return;
        }

        long retryNanos = Math.max(videoCacheProperties.getLockRetryInterval().toNanos(), TimeUnit.MILLISECONDS.toNanos(1));
        long sleepMillis = Math.max(1, TimeUnit.NANOSECONDS.toMillis(Math.min(remainingNanos, retryNanos)));
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public List<Video> loadVideoByChannel(String channelId) {
        // Cache Aside: Redis 먼저 조회
        String key = getVideoListKey(channelId);
        String json = stringRedisTemplate.opsForValue().get(key);

        if (json != null) {
            try {
                return objectMapper.readValue(json, new TypeReference<List<Video>>() {});
            } catch (Exception e) {
                log.warn("캐시 역직렬화 실패, DB에서 조회합니다. key={}", key, e);
            }
        }

        // 캐시 미스: JPA 조회 후 캐시 저장
        List<Video> videos = videoCustomRepository.findByChannelId(channelId).stream()
                .map(VideoJpaEntity::toDomain)
                .toList();

        try {
            stringRedisTemplate.opsForValue().set(
                    key, objectMapper.writeValueAsString(videos), Duration.ofHours(1));
        } catch (Exception e) {
            log.warn("캐시 저장 실패. key={}", key, e);
        }

        return videos;
    }

    @Override
    public void saveVideo(Video video) {
        videoJpaRepository.save(VideoJpaEntity.from(video));

        // 쓰기 후 캐시 무효화
        stringRedisTemplate.delete(getVideoKey(video.getId()));
        stringRedisTemplate.delete(getVideoListKey(video.getChannelId()));
    }

    @Override
    public void incrementViewCount(String videoId) {
        var videoViewCountKey = getVideoViewCountKey(videoId);
        redisTemplate.opsForValue().increment(videoViewCountKey);
    }

    @Override
    public Long getViewCount(String videoId) {
        var videoViewCountKey = getVideoViewCountKey(videoId);
        var viewCont = redisTemplate.opsForValue().get(videoViewCountKey);
        return viewCont == null ? 0 : viewCont;
    }

    @Override
    public List<String> getAllVideoIdsWithViewCount() {
        var members = stringRedisTemplate.opsForSet().members(RedisKeyGenerator.getVideoViewCountSetKey());
        if (members == null) {
            return Collections.emptyList();
        }

        return members.stream().toList();
    }

    @Override
    public void syncViewCount(String videoId) {
        videoJpaRepository.findById(videoId)
                .ifPresent(videoJpaEntity -> {
                    videoJpaEntity.updateViewCount(redisTemplate.opsForValue().get(
                            getVideoViewCountKey(videoId)));
                    videoJpaRepository.save(videoJpaEntity);

                    redisTemplate.opsForSet().remove(RedisKeyGenerator.getVideoViewCountSetKey(), videoId);
                });
    }
}

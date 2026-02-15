package com.videoservice.manager;

import static com.videoservice.manager.redis.common.RedisKeyGenerator.getVideoKey;
import static com.videoservice.manager.redis.common.RedisKeyGenerator.getVideoListKey;
import static com.videoservice.manager.redis.common.RedisKeyGenerator.getVideoViewCountKey;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoservice.manager.jpa.video.VideoCustomRepository;
import com.videoservice.manager.jpa.video.VideoJpaEntity;
import com.videoservice.manager.jpa.video.VideoJpaRepository;
import com.videoservice.manager.redis.common.RedisKeyGenerator;
import com.videoservice.manager.video.Video;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
    private final ObjectMapper objectMapper;

    public VideoPersistenceAdapter(
            VideoJpaRepository videoJpaRepository,
            VideoCustomRepository videoCustomRepository,
            RedisTemplate<String, Long> redisTemplate,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper
    ) {
        this.videoJpaRepository = videoJpaRepository;
        this.videoCustomRepository = videoCustomRepository;
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Video loadVideo(String videoId) {
        // Cache Aside: Redis 먼저 조회
        String key = getVideoKey(videoId);
        String json = stringRedisTemplate.opsForValue().get(key);

        if (json != null) {
            try {
                return objectMapper.readValue(json, Video.class);
            } catch (Exception e) {
                log.warn("캐시 역직렬화 실패, DB에서 조회합니다. key={}", key, e);
            }
        }

        // 캐시 미스: JPA 조회 후 캐시 저장
        Video video = videoJpaRepository.findById(videoId)
                .map(VideoJpaEntity::toDomain)
                .orElseThrow();

        try {
            stringRedisTemplate.opsForValue().set(
                    key, objectMapper.writeValueAsString(video), Duration.ofHours(1));
        } catch (Exception e) {
            log.warn("캐시 저장 실패. key={}", key, e);
        }

        return video;
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

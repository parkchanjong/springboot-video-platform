package com.videoservice.manager;

import com.videoservice.manager.redis.common.RedisKeyGenerator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class VideoLikePersistenceAdapter implements VideoLikePort {
    private final StringRedisTemplate stringRedisTemplate;

    public VideoLikePersistenceAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Long addVideoLike(String videoId, String userId) {
        var setOps = stringRedisTemplate.opsForSet();
        return setOps.add(RedisKeyGenerator.getVideoLikeKey(videoId), userId);
    }

    @Override
    public Long removeVideoLike(String videoId, String userId) {
        var setOps = stringRedisTemplate.opsForSet();
        return setOps.remove(RedisKeyGenerator.getVideoLikeKey(videoId), userId);
    }

    @Override
    public Boolean isVideoLikeMember(String videoId, String userId) {
        return stringRedisTemplate.opsForSet().isMember(RedisKeyGenerator.getVideoLikeKey(videoId), userId);
    }

    @Override
    public Long getVideoLikeCount(String videoId) {
        return stringRedisTemplate.opsForSet().size(RedisKeyGenerator.getVideoLikeKey(videoId));
    }
}

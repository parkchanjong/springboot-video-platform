package com.videoservice.manager;

import com.videoservice.manager.common.RedisKeyGenerator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserSessionPersistenceAdapter implements UserSessionPort {
    private final StringRedisTemplate stringRedisTemplate;

    public UserSessionPersistenceAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String getUserId(String authKey) {
        return stringRedisTemplate.opsForValue().get(RedisKeyGenerator.getUserSessionKey(authKey));
    }
}
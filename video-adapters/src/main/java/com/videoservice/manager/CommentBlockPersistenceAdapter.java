package com.videoservice.manager;

import com.videoservice.manager.redis.common.RedisKeyGenerator;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CommentBlockPersistenceAdapter implements CommentBlockPort {
    private final StringRedisTemplate stringRedisTemplate;

    public CommentBlockPersistenceAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void saveUserCommentBlock(String userId, String commentId) {
        stringRedisTemplate.opsForSet().add(RedisKeyGenerator.getUserCommentBlock(userId), commentId);
    }

    @Override
    public Set<String> getUserCommentBlocks(String userId) {
        return stringRedisTemplate.opsForSet().members(RedisKeyGenerator.getUserCommentBlock(userId));
    }
}
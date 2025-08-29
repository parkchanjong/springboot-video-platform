package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.videoservice.manager.common.RedisKeyGenerator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class CommentBlockPersistenceAdapterTest {
    private CommentBlockPersistenceAdapter sut;

    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final SetOperations<String, String> setOperations = mock(SetOperations.class);

    @BeforeEach
    void setUp() {
        sut = new CommentBlockPersistenceAdapter(stringRedisTemplate);

        given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
    }

    @Test
    void testSaveUserCommentBlock() {
        var userId = "userId";
        var commentId = "commentId";

        sut.saveUserCommentBlock(userId, commentId);

        verify(setOperations).add(RedisKeyGenerator.getUserCommentBlock("userId"), "commentId");
    }

    @Test
    void testGetUserCommentBlock() {
        var userId = "userId";
        given(setOperations.members(any())).willReturn(Set.of("comment1", "comment2", "comment3"));

        var result = sut.getUserCommentBlocks(userId);

        then(result)
                .contains("comment1", "comment2", "comment3");
    }
}
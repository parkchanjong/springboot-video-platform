package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;

class CommentLikePersistenceAdapterTest {
    private CommentLikePersistenceAdapter sut;

    private final RedisTemplate<String, Long> redisTemplate = mock(RedisTemplate.class, Mockito.RETURNS_DEEP_STUBS);

    @BeforeEach
    void setUp() {
        sut = new CommentLikePersistenceAdapter(redisTemplate);
    }

    @Test
    void testGetCommentLikeCount() {
        var commentId = "commentId";
        given(redisTemplate.opsForValue().get(any())).willReturn(2L);

        var result = sut.getCommentLikeCount(commentId);

        then(result).isEqualTo(2L);
    }

    @Test
    void testGinveNullWhenGetCommentLikeCountThenReturnZero() {
        var commentId = "commentId";
        given(redisTemplate.opsForValue().get(any())).willReturn(null);

        var result = sut.getCommentLikeCount(commentId);

        then(result).isEqualTo(0L);
    }
}
package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.videoservice.manager.redis.common.RedisKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class VideoLikePersistenceAdapterTest {
    private VideoLikePersistenceAdapter sut;

    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class, Mockito.RETURNS_DEEP_STUBS);
    private final SetOperations<String, String> setOperations = mock(SetOperations.class);

    @BeforeEach
    void setUp() {
        sut = new VideoLikePersistenceAdapter(stringRedisTemplate);

        given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
    }

    @Test
    void testAddVideoLike() {
        given(setOperations.add(any(), any())).willReturn(5L);

        var result = sut.addVideoLike("videoId", "userId");

        then(result).isEqualTo(5L);
        verify(setOperations).add(RedisKeyGenerator.getVideoLikeKey("videoId"), "userId");
    }

    @Test
    void testRemoveVideoLike() {
        given(setOperations.remove(any(), any())).willReturn(4L);

        var result = sut.removeVideoLike("videoId", "userId");

        then(result).isEqualTo(4L);
        verify(setOperations).remove(RedisKeyGenerator.getVideoLikeKey("videoId"), "userId");
    }

    @Test
    void testIsVideoLikeMember() {
        given(setOperations.isMember(any(), anyString())).willReturn(true);

        var result = sut.isVideoLikeMember("videoId", "userId");

        then(result).isTrue();
        verify(setOperations).isMember(RedisKeyGenerator.getVideoLikeKey("videoId"), "userId");
    }

    @Test
    void testCountVideoLike() {
        given(setOperations.size(any())).willReturn(10L);

        var result = sut.getVideoLikeCount("videoId");

        then(result).isEqualTo(10L);
        verify(setOperations).size(RedisKeyGenerator.getVideoLikeKey("videoId"));
    }
}
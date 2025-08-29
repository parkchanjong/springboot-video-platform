package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VideoLikeServiceTest {
    private VideoLikeService sut;

    private final VideoLikePort videoLikePort = mock(VideoLikePort.class);

    @BeforeEach
    void setUp() {
        sut = new VideoLikeService(videoLikePort);
    }

    @Test
    @DisplayName("user가 video 에 대해 좋아요를 하면 user-video like 추가")
    void testLikeVideo() {
        // given
        given(videoLikePort.addVideoLike(any(), any())).willReturn(3L);
        // when
        var result = sut.likeVideo("videoId", "userId");
        // then
        then(result).isEqualTo(3L);
    }

    @Test
    @DisplayName("user가 video 에 대해 좋아요를 취소하면 user-video like 제거")
    void testUnlikeVideo() {
        // given
        given(videoLikePort.removeVideoLike(any(), any())).willReturn(4L);
        // when
        var result = sut.unlikeVideo("videoId", "userId");
        // then
        then(result).isEqualTo(4L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("user가 video에 대해 좋아요를 했는지 여부 반환")
    void testIsLikeVideo(Boolean value) {
        // given
        given(videoLikePort.isVideoLikeMember(any(), any())).willReturn(value);

        var result = sut.isLikedVideo("videoId", "userId");

        then(result).isEqualTo(value);
    }

    @Test
    void testGetCountVideoLike() {
        given(videoLikePort.getVideoLikeCount(any())).willReturn(20L);

        var result = sut.getVideoLikeCount("videoId");

        then(result).isEqualTo(20L);
    }
}
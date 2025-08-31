package com.videoservice.manager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.videoservice.manager.LoadUserPort;
import com.videoservice.manager.UserSessionPort;
import com.videoservice.manager.VideoLikeUseCase;
import com.videoservice.manager.attribute.HeaderAttribute;
import com.videoservice.manager.domain.user.UserFixtures;
import com.videoservice.manager.user.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VideoRateApiController.class)
class VideoRateApiControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoLikeUseCase videoLikeUseCase;
    @MockBean
    private UserSessionPort userSessionPort;
    @MockBean
    private LoadUserPort loadUserPort;

    private String authKey;
    private User user;

    @BeforeEach
    void setUp() {
        authKey = UUID.randomUUID().toString();
        user = UserFixtures.stub();

        given(userSessionPort.getUserId(anyString())).willReturn(user.getId());
        given(loadUserPort.loadUser(anyString())).willReturn(Optional.of(user));
    }

    @Nested
    @DisplayName("POST /api/v1/videos/rate")
    class RateVideo {
        @Test
        @DisplayName("video 좋아요")
        void testLikeVideo() throws Exception {
            mockMvc
                    .perform(
                            post("/api/v1/videos/rate?videoId={videoId}&rating={rate}", "videoId", "like")
                                    .header(HeaderAttribute.X_AUTH_KEY, authKey)
                    )
                    .andExpect(
                            status().isOk()
                    );

            verify(videoLikeUseCase).likeVideo("videoId", user.getId());
        }

        @Test
        @DisplayName("video 좋아요 취소")
        void testUnlikeVideo() throws Exception {
            mockMvc
                    .perform(
                            post("/api/v1/videos/rate?videoId={videoId}&rating={rate}", "videoId", "none")
                                    .header(HeaderAttribute.X_AUTH_KEY, authKey)
                    )
                    .andExpect(
                            status().isOk()
                    );

            verify(videoLikeUseCase).unlikeVideo("videoId", user.getId());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/rate")
    class GetVideoRate {
        @ParameterizedTest
        @CsvSource(value = {"true,like", "false,none"})
        @DisplayName("좋아요한 비디오는 rate=like 를 반환")
        void testGetVideoLikeRate(Boolean likedVideo, String rate) throws Exception {
            given(videoLikeUseCase.isLikedVideo(any(), any())).willReturn(likedVideo);

            mockMvc
                    .perform(
                            get("/api/v1/videos/rate?videoId={videoId}", "videoId")
                                    .header(HeaderAttribute.X_AUTH_KEY, authKey)
                    )
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.videoId").value("videoId"),
                            jsonPath("$.rate").value(rate)
                    );
        }
    }
}
package com.videoservice.manager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoservice.manager.VideoUseCase;
import com.videoservice.manager.domain.video.VideoFixtures;
import com.videoservice.manager.dto.VideoRequest;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VideoApiController.class)
class VideoApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private VideoUseCase videoUseCase;

    @Nested
    @DisplayName("GET /api/v1/videos/{videoId}")
    class GetVideoTest {

        @Test
        @DisplayName("vidoe 가 있으면 200 OK")
        void testGetVideo() throws Exception {
            // Given
            var videoId = "videoId";
            given(videoUseCase.getVideo(any())).willReturn(VideoFixtures.stub(videoId));

            // When
            mockMvc
                    .perform(
                            get("/api/v1/videos/{videoId}", videoId)
                    )
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.id").value(videoId)
                    );
        }
    }

    @Nested
    @DisplayName("GET /api/v1/videos?channelId={channelId}")
    class ListVideoTest {

        @Test
        @DisplayName("200 OK, 목록 반환")
        void testListVideo() throws Exception {
            // Given
            var channelId = "channelId";
            var list = LongStream.range(1L, 4L)
                    .mapToObj(i -> VideoFixtures.stub("videoId" + i))
                    .toList();
            given(videoUseCase.listVideos(any())).willReturn(list);

            // When
            mockMvc
                    .perform(
                            get("/api/v1/videos?channelId={channelId}", channelId)
                    )
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.size()").value(3),
                            jsonPath("$[0].channelId").value(channelId)
                    );
        }
    }

    @Nested
    @DisplayName("POST /api/v1/videos")
    class CreateVideo {

        @Test
        @DisplayName("200 OK, 생성된 Video id 를 반환")
        void testCreateVideo() throws Exception {
            // given
            var videoRequest = new VideoRequest("title", "desc", "https://example.com/image.jpg",
                    "channelId");
            given(videoUseCase.createVideo(any())).willReturn(VideoFixtures.stub("videoId"));

            // when
            mockMvc
                    .perform(
                            post("/api/v1/videos")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(videoRequest))
                    )
                    .andExpectAll(
                            status().isOk()
                    );

            // then
            verify(videoUseCase).createVideo(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/videos/view")
    class VideoViewCountTest {

        @Test
        @DisplayName("200 OK, 조회수 증가")
        void testViewCount() throws Exception {
            // Given
            var videoId = "videoId";
            willDoNothing().given(videoUseCase).increaseViewCount(any());

            // When
            mockMvc
                    .perform(
                            post("/api/v1/videos/{videoId}/view", videoId)
                    )
                    .andExpectAll(
                            status().isOk()
                    );

            // Then
            verify(videoUseCase).increaseViewCount(videoId);
        }
    }
}
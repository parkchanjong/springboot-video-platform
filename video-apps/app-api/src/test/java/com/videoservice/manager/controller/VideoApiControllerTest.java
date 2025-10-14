package com.videoservice.manager.controller;

import static com.videoservice.manager.RestDocsUtils.requestPreprocessor;
import static com.videoservice.manager.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

import com.videoservice.manager.RestDocsTest;
import com.videoservice.manager.VideoUseCase;
import com.videoservice.manager.domain.video.VideoFixtures;
import com.videoservice.manager.dto.VideoRequest;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.payload.JsonFieldType;

class VideoApiControllerTest extends RestDocsTest {

    private VideoUseCase videoUseCase;

    private VideoApiController controller;

    @BeforeEach
    void setUp() {
        videoUseCase = mock(VideoUseCase.class);
        controller = new VideoApiController(videoUseCase);

        mockMvc = mockController(controller);
    }

    @Test
    @DisplayName("GET /api/v1/videos/{videoId} returns video details")
    void getVideo() {
        var videoId = "video-123";
        when(videoUseCase.getVideo(videoId)).thenReturn(VideoFixtures.stub(videoId));

        given().contentType(ContentType.JSON)
                .get("/api/v1/videos/{videoId}", videoId)
                .then()
                .status(HttpStatus.OK)
                .apply(document("videos-get", requestPreprocessor(), responsePreprocessor(),
                        pathParameters(
                                parameterWithName("videoId").description(
                                        "Video identifier to fetch")),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.STRING)
                                        .description("Video identifier"),
                                fieldWithPath("title").type(JsonFieldType.STRING)
                                        .description("Video title"),
                                fieldWithPath("description").type(JsonFieldType.STRING)
                                        .description("Video description"),
                                fieldWithPath("thumbnailUrl").type(JsonFieldType.STRING)
                                        .description("Thumbnail URL"),
                                fieldWithPath("fileUrl").type(JsonFieldType.STRING)
                                        .description("Streamable file URL"),
                                fieldWithPath("channelId").type(JsonFieldType.STRING)
                                        .description("Channel identifier"),
                                fieldWithPath("viewCount").type(JsonFieldType.NUMBER)
                                        .description("Total view count"),
                                fieldWithPath("likeCount").type(JsonFieldType.NUMBER)
                                        .description("Total like count"),
                                fieldWithPath("publishedAt").type(JsonFieldType.STRING)
                                        .description("Published timestamp (ISO-8601)"))));
    }

    @Test
    @DisplayName("GET /api/v1/videos?channelId= lists channel videos")
    void listVideos() {
        var channelId = "channel-001";
        var videos = List.of(
                VideoFixtures.stub("video-001"),
                VideoFixtures.stub("video-002")
        );
        when(videoUseCase.listVideos(channelId)).thenReturn(videos);

        given().contentType(ContentType.JSON)
                .queryParam("channelId", channelId)
                .get("/api/v1/videos")
                .then()
                .status(HttpStatus.OK)
                .apply(document("videos-list", requestPreprocessor(), responsePreprocessor(),
                        queryParameters(
                                parameterWithName("channelId").description(
                                        "Channel identifier whose videos are requested")),
                        responseFields(
                                fieldWithPath("[]").type(JsonFieldType.ARRAY)
                                        .description("List of videos for the channel"),
                                fieldWithPath("[].id").type(JsonFieldType.STRING)
                                        .description("Video identifier"),
                                fieldWithPath("[].title").type(JsonFieldType.STRING)
                                        .description("Video title"),
                                fieldWithPath("[].description").type(JsonFieldType.STRING)
                                        .description("Video description"),
                                fieldWithPath("[].thumbnailUrl").type(JsonFieldType.STRING)
                                        .description("Thumbnail URL"),
                                fieldWithPath("[].fileUrl").type(JsonFieldType.STRING)
                                        .description("Streamable file URL"),
                                fieldWithPath("[].channelId").type(JsonFieldType.STRING)
                                        .description("Channel identifier"),
                                fieldWithPath("[].viewCount").type(JsonFieldType.NUMBER)
                                        .description("Total view count"),
                                fieldWithPath("[].likeCount").type(JsonFieldType.NUMBER)
                                        .description("Total like count"),
                                fieldWithPath("[].publishedAt").type(JsonFieldType.STRING)
                                        .description("Published timestamp (ISO-8601)"))));
    }

    @Test
    @DisplayName("POST /api/v1/videos creates a video")
    void createVideo() {
        var request = new VideoRequest(
                "video title",
                "video description",
                "https://cdn.video/thumbnail.jpg",
                "channel-001"
        );
        when(videoUseCase.createVideo(any())).thenReturn(VideoFixtures.stub("video-001"));

        given().contentType(ContentType.JSON)
                .body(request)
                .post("/api/v1/videos")
                .then()
                .status(HttpStatus.OK)
                .apply(document("videos-create", requestPreprocessor(), responsePreprocessor(),
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING)
                                        .description("Video title"),
                                fieldWithPath("description").type(JsonFieldType.STRING)
                                        .description("Video description"),
                                fieldWithPath("thumbnailUrl").type(JsonFieldType.STRING)
                                        .description("Thumbnail image URL"),
                                fieldWithPath("channelId").type(JsonFieldType.STRING)
                                        .description("Channel identifier that owns the video")),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.STRING)
                                        .description("Created video identifier"))));
    }

    @Test
    @DisplayName("POST /api/v1/videos/{videoId}/view increases view count")
    void increaseViewCount() {
        var videoId = "video-123";

        given().contentType(ContentType.JSON)
                .post("/api/v1/videos/{videoId}/view", videoId)
                .then()
                .status(HttpStatus.OK)
                .apply(document("videos-view-increase", requestPreprocessor(),
                        responsePreprocessor(),
                        pathParameters(
                                parameterWithName("videoId").description(
                                        "Video identifier whose view count should increase"))));
    }
}

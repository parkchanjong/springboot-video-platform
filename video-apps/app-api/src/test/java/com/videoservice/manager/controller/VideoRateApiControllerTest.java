package com.videoservice.manager.controller;

import static com.videoservice.manager.RestDocsUtils.requestPreprocessor;
import static com.videoservice.manager.RestDocsUtils.responsePreprocessor;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

import com.videoservice.manager.RestDocsTest;
import com.videoservice.manager.VideoLikeUseCase;
import com.videoservice.manager.attribute.HeaderAttribute;
import com.videoservice.manager.domain.user.UserFixtures;
import com.videoservice.manager.user.User;
import com.videoservice.manager.video.VideoRate;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.payload.JsonFieldType;

class VideoRateApiControllerTest extends RestDocsTest {

    private VideoLikeUseCase videoLikeUseCase;

    private VideoRateApiController controller;

    private User stubUser;

    @BeforeEach
    void setUp() {
        videoLikeUseCase = mock(VideoLikeUseCase.class);
        controller = new VideoRateApiController(videoLikeUseCase);
        stubUser = UserFixtures.stub();

        mockMvc = mockController(controller);
    }

    @Test
    @DisplayName("POST /api/v1/videos/rate?rating=like likes the video")
    void rateVideoLike() {
        var videoId = "video-123";

        given().contentType(ContentType.JSON)
                .header(HeaderAttribute.X_AUTH_KEY, "auth-key-001")
                .queryParam("videoId", videoId)
                .queryParam("rating", VideoRate.like)
                .post("/api/v1/videos/rate")
                .then()
                .status(HttpStatus.OK)
                .apply(document("videos-rate-like", requestPreprocessor(), responsePreprocessor(),
                        requestHeaders(
                                headerWithName(HeaderAttribute.X_AUTH_KEY).description(
                                        "Authentication key for the current user")),
                        queryParameters(
                                parameterWithName("videoId").description("Video identifier"),
                                parameterWithName("rating").description(
                                        "Desired rating (like or none)"))));
    }

    @Test
    @DisplayName("POST /api/v1/videos/rate?rating=none unlikes the video")
    void rateVideoUnlike() {
        var videoId = "video-123";

        given().contentType(ContentType.JSON)
                .header(HeaderAttribute.X_AUTH_KEY, "auth-key-001")
                .queryParam("videoId", videoId)
                .queryParam("rating", VideoRate.none)
                .post("/api/v1/videos/rate")
                .then()
                .status(HttpStatus.OK)
                .apply(document("videos-rate-none", requestPreprocessor(), responsePreprocessor(),
                        requestHeaders(
                                headerWithName(HeaderAttribute.X_AUTH_KEY).description(
                                        "Authentication key for the current user")),
                        queryParameters(
                                parameterWithName("videoId").description("Video identifier"),
                                parameterWithName("rating").description(
                                        "Desired rating (like or none)"))));
    }

    @Test
    @DisplayName("GET /api/v1/videos/rate returns the current rating state")
    void getRate() {
        var videoId = "video-123";
        when(videoLikeUseCase.isLikedVideo(videoId, stubUser.getId())).thenReturn(true);

        given().contentType(ContentType.JSON)
                .header(HeaderAttribute.X_AUTH_KEY, "auth-key-001")
                .queryParam("videoId", videoId)
                .get("/api/v1/videos/rate")
                .then()
                .status(HttpStatus.OK)
                .body("videoId", equalTo(videoId))
                .body("rate", equalTo(VideoRate.like.name()))
                .apply(document("videos-rate-get", requestPreprocessor(), responsePreprocessor(),
                        requestHeaders(
                                headerWithName(HeaderAttribute.X_AUTH_KEY).description(
                                        "Authentication key for the current user")),
                        queryParameters(
                                parameterWithName("videoId").description("Video identifier")),
                        responseFields(
                                fieldWithPath("videoId").type(JsonFieldType.STRING)
                                        .description("Video identifier"),
                                fieldWithPath("rate").type(JsonFieldType.STRING)
                                        .description("Current rating (like or none)"))));
    }
}

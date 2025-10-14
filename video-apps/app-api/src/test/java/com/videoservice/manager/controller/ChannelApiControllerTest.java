package com.videoservice.manager.controller;

import static com.videoservice.manager.RestDocsUtils.requestPreprocessor;
import static com.videoservice.manager.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

import com.videoservice.manager.ChannelUseCase;
import com.videoservice.manager.RestDocsTest;
import com.videoservice.manager.domain.channel.ChannelFixtures;
import com.videoservice.manager.dto.ChannelRequest;
import com.videoservice.manager.dto.ChannelRequest.ChannelSnippetRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.payload.JsonFieldType;

class ChannelApiControllerTest extends RestDocsTest {

    private ChannelUseCase channelUseCase;

    private ChannelApiController controller;

    @BeforeEach
    void setUp() {
        channelUseCase = mock(ChannelUseCase.class);
        controller = new ChannelApiController(channelUseCase);
        mockMvc = mockController(controller);
    }

    @Test
    void createChannel() {
        when(channelUseCase.createChannel(any())).thenReturn(ChannelFixtures.stub("channelId"));

        given().contentType(ContentType.JSON)
                .body(new ChannelRequest(
                        new ChannelSnippetRequest("channel title", "channel description",
                                "https://cdn.video/thumbnail.jpg"),
                        "content-owner-1"))
                .post("/api/v1/channels")
                .then()
                .status(HttpStatus.OK)
                .apply(document("channels-create", requestPreprocessor(), responsePreprocessor(),
                        requestFields(
                                subsectionWithPath("snippet").type(JsonFieldType.OBJECT)
                                        .description("Channel snippet payload"),
                                fieldWithPath("snippet.title").type(JsonFieldType.STRING)
                                        .description("Channel title"),
                                fieldWithPath("snippet.description").type(JsonFieldType.STRING)
                                        .description("Channel description"),
                                fieldWithPath("snippet.thumbnailUrl").type(JsonFieldType.STRING)
                                        .description("Channel thumbnail URL"),
                                fieldWithPath("contentOwnerId").type(JsonFieldType.STRING)
                                        .description("Content owner identifier")),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.STRING)
                                        .description("Generated channel identifier"))));
    }

    @Test
    void updateChannel() {
        when(channelUseCase.updateChannel(eq("channel-123"), any())).thenReturn(
                ChannelFixtures.stub("channelId"));

        given().contentType(ContentType.JSON)
                .body(new ChannelRequest(
                        new ChannelSnippetRequest("updated title", "updated description",
                                "https://cdn.video/thumbnail-updated.jpg"),
                        "content-owner-1"))
                .put("/api/v1/channels/{channelId}", "channel-123")
                .then()
                .status(HttpStatus.OK)
                .apply(document("channels-update", requestPreprocessor(), responsePreprocessor(),
                        pathParameters(
                                parameterWithName("channelId").description("Channel identifier")),
                        requestFields(
                                subsectionWithPath("snippet").type(JsonFieldType.OBJECT)
                                        .description("Channel snippet payload"),
                                fieldWithPath("snippet.title").type(JsonFieldType.STRING)
                                        .description("Channel title"),
                                fieldWithPath("snippet.description").type(JsonFieldType.STRING)
                                        .description("Channel description"),
                                fieldWithPath("snippet.thumbnailUrl").type(JsonFieldType.STRING)
                                        .description("Channel thumbnail URL"),
                                fieldWithPath("contentOwnerId").type(JsonFieldType.STRING)
                                        .description("Content owner identifier"))));
    }

    @Test
    void getChannel() {
        when(channelUseCase.getChannel("channel-123")).thenReturn(
                ChannelFixtures.stub("channelId"));

        given().contentType(ContentType.JSON)
                .get("/api/v1/channels/{channelId}", "channel-123")
                .then()
                .status(HttpStatus.OK)
                .apply(document("channels-get", requestPreprocessor(), responsePreprocessor(),
                        pathParameters(
                                parameterWithName("channelId").description("Channel identifier")),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.STRING)
                                        .description("Channel identifier"),
                                subsectionWithPath("snippet").type(JsonFieldType.OBJECT)
                                        .description("Channel snippet information"),
                                fieldWithPath("snippet.title").type(JsonFieldType.STRING)
                                        .description("Channel title"),
                                fieldWithPath("snippet.description").type(JsonFieldType.STRING)
                                        .description("Channel description"),
                                fieldWithPath("snippet.thumbnailUrl").type(JsonFieldType.STRING)
                                        .description("Channel thumbnail URL"),
                                fieldWithPath("snippet.publishedAt").type(JsonFieldType.STRING)
                                        .description("Channel published timestamp"),
                                subsectionWithPath("statistics").type(JsonFieldType.OBJECT)
                                        .description("Channel statistics"),
                                fieldWithPath("statistics.videoCount").type(JsonFieldType.NUMBER)
                                        .description("Number of videos in the channel"),
                                fieldWithPath("statistics.subscriberCount").type(
                                                JsonFieldType.NUMBER)
                                        .description("Number of channel subscribers"),
                                fieldWithPath("statistics.commentCount").type(JsonFieldType.NUMBER)
                                        .description("Number of comments across channel videos"),
                                fieldWithPath("contentOwnerId").type(JsonFieldType.STRING)
                                        .description("Content owner identifier"))));
    }
}

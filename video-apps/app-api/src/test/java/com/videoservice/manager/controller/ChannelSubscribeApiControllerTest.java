package com.videoservice.manager.controller;

import static com.videoservice.manager.RestDocsUtils.requestPreprocessor;
import static com.videoservice.manager.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

import com.videoservice.manager.RestDocsTest;
import com.videoservice.manager.SubscribeUseCase;
import com.videoservice.manager.attribute.HeaderAttribute;
import com.videoservice.manager.domain.channel.ChannelFixtures;
import com.videoservice.manager.domain.user.UserFixtures;
import com.videoservice.manager.user.User;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.payload.JsonFieldType;

class ChannelSubscribeApiControllerTest extends RestDocsTest {

    private SubscribeUseCase subscribeUseCase;

    private ChannelSubscribeApiController controller;

    private User stubUser;

    @BeforeEach
    void setUp() {
        subscribeUseCase = mock(SubscribeUseCase.class);
        controller = new ChannelSubscribeApiController(subscribeUseCase);
        stubUser = UserFixtures.stub();

        mockMvc = mockController(controller);
    }

    @Test
    @DisplayName("POST /api/v1/subscribe creates a subscription")
    void subscribeChannel() {
        var channelId = "channel-123";
        var subscribeId = "subscribe-456";

        when(subscribeUseCase.subscribeChannel(eq(channelId), eq(stubUser.getId()))).thenReturn(
                subscribeId);

        given().contentType(ContentType.JSON)
                .header(HeaderAttribute.X_AUTH_KEY, "auth-key-001")
                .queryParam("channelId", channelId)
                .post("/api/v1/subscribe")
                .then()
                .status(HttpStatus.OK)
                .apply(document("subscribe-create", requestPreprocessor(), responsePreprocessor(),
                        requestHeaders(
                                headerWithName(HeaderAttribute.X_AUTH_KEY).description(
                                        "Authentication key")),
                        pathParameters(
                                parameterWithName("channelId").description(
                                        "Identifier of the channel to subscribe")),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.STRING)
                                        .description("Subscription identifier"))));
    }

    @Test
    @DisplayName("GET /api/v1/subscribe/mine lists user subscriptions")
    void listSubscribeChannelByUser() {
        var channels = List.of(
                ChannelFixtures.stub("channel-1"),
                ChannelFixtures.stub("channel-2")
        );
        when(subscribeUseCase.listSubscribeChannel(eq(stubUser.getId()))).thenReturn(channels);

        given().contentType(ContentType.JSON)
                .header(HeaderAttribute.X_AUTH_KEY, "auth-key-001")
                .get("/api/v1/subscribe/mine")
                .then()
                .status(HttpStatus.OK)
                .apply(document("subscribe-list", requestPreprocessor(), responsePreprocessor(),
                        requestHeaders(
                                headerWithName(HeaderAttribute.X_AUTH_KEY).description(
                                        "Authentication key")),
                        responseFields(
                                fieldWithPath("[]").type(JsonFieldType.ARRAY)
                                        .description("Subscribed channels"),
                                fieldWithPath("[].id").type(JsonFieldType.STRING)
                                        .description("Channel identifier"),
                                subsectionWithPath("[].snippet").type(JsonFieldType.OBJECT)
                                        .description("Channel snippet information"),
                                fieldWithPath("[].snippet.title").type(JsonFieldType.STRING)
                                        .description("Channel title"),
                                fieldWithPath("[].snippet.description").type(JsonFieldType.STRING)
                                        .description("Channel description"),
                                fieldWithPath("[].snippet.thumbnailUrl").type(JsonFieldType.STRING)
                                        .description("Channel thumbnail URL"),
                                fieldWithPath("[].snippet.publishedAt").type(JsonFieldType.STRING)
                                        .description("Channel published timestamp"),
                                subsectionWithPath("[].statistics").type(JsonFieldType.OBJECT)
                                        .description("Channel statistics"),
                                fieldWithPath("[].statistics.videoCount").type(JsonFieldType.NUMBER)
                                        .description("Number of videos"),
                                fieldWithPath("[].statistics.subscriberCount").type(
                                                JsonFieldType.NUMBER)
                                        .description("Number of subscribers"),
                                fieldWithPath("[].statistics.commentCount").type(
                                                JsonFieldType.NUMBER)
                                        .description("Number of comments"),
                                fieldWithPath("[].contentOwnerId").type(JsonFieldType.STRING)
                                        .description("Owner identifier"))));
    }
}

package com.videoservice.manager.controller;

import static com.videoservice.manager.RestDocsUtils.requestPreprocessor;
import static com.videoservice.manager.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

import com.videoservice.manager.CommentUseCase;
import com.videoservice.manager.RestDocsTest;
import com.videoservice.manager.attribute.HeaderAttribute;
import com.videoservice.manager.domain.comment.CommentFixtures;
import com.videoservice.manager.domain.comment.CommentResponseFixtures;
import com.videoservice.manager.domain.user.UserFixtures;
import com.videoservice.manager.dto.CommentRequest;
import com.videoservice.manager.user.User;
import io.restassured.http.ContentType;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.payload.JsonFieldType;

class CommentApiControllerTest extends RestDocsTest {

    private CommentUseCase commentUseCase;

    private CommentApiController controller;

    private User stubUser;

    @BeforeEach
    void setUp() {
        commentUseCase = mock(CommentUseCase.class);
        controller = new CommentApiController(commentUseCase);
        stubUser = UserFixtures.stub();

        mockMvc = mockController(controller);
    }

    @Test
    @DisplayName("POST /api/v1/comments creates a comment")
    void createComment() {
        var request = new CommentRequest("channel-1", "video-1", "parent-0", "Great video!");
        when(commentUseCase.createComment(eq(stubUser), any())).thenReturn(
                CommentFixtures.stub("comment-1"));

        given().contentType(ContentType.JSON)
                .header(HeaderAttribute.X_AUTH_KEY, "auth-key-001")
                .body(request)
                .post("/api/v1/comments")
                .then()
                .status(HttpStatus.OK)
                .apply(document("comments-create", requestPreprocessor(), responsePreprocessor(),
                        requestHeaders(
                                headerWithName(HeaderAttribute.X_AUTH_KEY)
                                        .description("Authentication key for the current user")),
                        requestFields(
                                fieldWithPath("channelId").type(JsonFieldType.STRING)
                                        .description(
                                                "Channel identifier where the comment belongs"),
                                fieldWithPath("videoId").type(JsonFieldType.STRING)
                                        .description("Video identifier"),
                                fieldWithPath("parentId").type(JsonFieldType.STRING)
                                        .description("Parent comment identifier if this is a reply")
                                        .optional(),
                                fieldWithPath("text").type(JsonFieldType.STRING)
                                        .description("Comment content")),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.STRING)
                                        .description("Created comment identifier"))));
    }

    @Test
    @DisplayName("PUT /api/v1/comments/{commentId} updates a comment")
    void updateComment() {
        var commentId = "comment-1";
        var request = new CommentRequest("channel-1", "video-1", "parent-0", "Updated comment");
        when(commentUseCase.updateComment(eq(commentId), eq(stubUser), any()))
                .thenReturn(CommentFixtures.stub(commentId));

        given().contentType(ContentType.JSON)
                .header(HeaderAttribute.X_AUTH_KEY, "auth-key-001")
                .body(request)
                .put("/api/v1/comments/{commentId}", commentId)
                .then()
                .status(HttpStatus.OK)
                .apply(document("comments-update", requestPreprocessor(), responsePreprocessor(),
                        requestHeaders(
                                headerWithName(HeaderAttribute.X_AUTH_KEY)
                                        .description("Authentication key for the current user")),
                        pathParameters(
                                parameterWithName("commentId").description(
                                        "Comment identifier to update")),
                        requestFields(
                                fieldWithPath("channelId").type(JsonFieldType.STRING)
                                        .description(
                                                "Channel identifier where the comment belongs"),
                                fieldWithPath("videoId").type(JsonFieldType.STRING)
                                        .description("Video identifier"),
                                fieldWithPath("parentId").type(JsonFieldType.STRING)
                                        .description("Parent comment identifier if this is a reply")
                                        .optional(),
                                fieldWithPath("text").type(JsonFieldType.STRING)
                                        .description("Updated comment content")),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.STRING)
                                        .description("Updated comment identifier"))));
    }

    @Test
    @DisplayName("DELETE /api/v1/comments/{commentId} removes a comment")
    void deleteComment() {
        var commentId = "comment-1";

        given().contentType(ContentType.JSON)
                .header(HeaderAttribute.X_AUTH_KEY, "auth-key-001")
                .delete("/api/v1/comments/{commentId}", commentId)
                .then()
                .status(HttpStatus.OK)
                .apply(document("comments-delete", requestPreprocessor(), responsePreprocessor(),
                        requestHeaders(
                                headerWithName(HeaderAttribute.X_AUTH_KEY)
                                        .description("Authentication key for the current user")),
                        pathParameters(
                                parameterWithName("commentId").description(
                                        "Comment identifier to delete"))));
    }

    @Test
    @DisplayName("GET /api/v1/comments returns a comment")
    void getComment() {
        var commentId = "comment-1";
        when(commentUseCase.getComment(commentId)).thenReturn(
                CommentResponseFixtures.stub(commentId));

        given().contentType(ContentType.JSON)
                .queryParam("commentId", commentId)
                .get("/api/v1/comments")
                .then()
                .status(HttpStatus.OK)
                .apply(document("comments-get", requestPreprocessor(), responsePreprocessor(),
                        queryParameters(
                                parameterWithName("commentId").description(
                                        "Comment identifier to fetch"))));
    }

    @Test
    @DisplayName("GET /api/v1/comments/list returns paged comments for a video")
    void listComments() {
        var videoId = "video-1";
        var order = "time";
        var offset = "2024-01-01T12:10:00";
        var maxSize = 10;
        when(commentUseCase.listComments(eq(stubUser), eq(videoId), eq(order), eq(offset),
                eq(maxSize)))
                .thenReturn(Collections.emptyList());

        given().contentType(ContentType.JSON)
                .header(HeaderAttribute.X_AUTH_KEY, "auth-key-001")
                .queryParam("videoId", videoId)
                .queryParam("order", order)
                .queryParam("offset", offset)
                .queryParam("maxSize", maxSize)
                .get("/api/v1/comments/list")
                .then()
                .status(HttpStatus.OK)
                .apply(document("comments-list", requestPreprocessor(), responsePreprocessor(),
                        requestHeaders(
                                headerWithName(HeaderAttribute.X_AUTH_KEY)
                                        .description("Authentication key for the current user")
                                        .optional()),
                        queryParameters(
                                parameterWithName("videoId").description("Video identifier"),
                                parameterWithName("order").description(
                                        "Sorting order (time or popularity)"),
                                parameterWithName("offset").description(
                                        "Cursor offset (ISO-8601 timestamp)"),
                                parameterWithName("maxSize").description(
                                        "Maximum number of comments to return"))
                ));
    }

    @Test
    @DisplayName("GET /api/v1/comments/reply returns replies for a parent comment")
    void listReplies() {
        var parentId = "comment-1";
        var offset = "2024-01-01T13:00:00";
        var maxSize = 5;
        when(commentUseCase.listReplies(eq(parentId), eq(offset), eq(maxSize)))
                .thenReturn(List.of(CommentResponseFixtures.stub("reply-1")));

        given().contentType(ContentType.JSON)
                .queryParam("parentId", parentId)
                .queryParam("offset", offset)
                .queryParam("maxSize", maxSize)
                .get("/api/v1/comments/reply")
                .then()
                .status(HttpStatus.OK)
                .apply(document("comments-replies", requestPreprocessor(), responsePreprocessor(),
                        queryParameters(
                                parameterWithName("parentId").description(
                                        "Parent comment identifier"),
                                parameterWithName("offset").description(
                                        "Cursor offset (ISO-8601 timestamp)"),
                                parameterWithName("maxSize").description(
                                        "Maximum number of replies to return"))
                ));
    }
}

package com.videoservice.manager.controller;

import static com.videoservice.manager.RestDocsUtils.requestPreprocessor;
import static com.videoservice.manager.RestDocsUtils.responsePreprocessor;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

import com.videoservice.manager.CommentBlockUseCase;
import com.videoservice.manager.RestDocsTest;
import com.videoservice.manager.attribute.HeaderAttribute;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CommentBlockApiControllerTest extends RestDocsTest {

    private CommentBlockUseCase commentBlockUseCase;

    private CommentBlockApiController controller;

    @BeforeEach
    void setUp() {
        commentBlockUseCase = mock(CommentBlockUseCase.class);
        controller = new CommentBlockApiController(commentBlockUseCase);

        mockMvc = mockController(controller);
    }

    @Test
    @DisplayName("POST /api/v1/comments/block blocks comments for the authenticated user")
    void blockComment() {
        var commentId = "comment-123";

        given().contentType(ContentType.JSON)
                .header(HeaderAttribute.X_AUTH_KEY, "auth-key-001")
                .queryParam("commentId", commentId)
                .post("/api/v1/comments/block")
                .then()
                .status(HttpStatus.OK)
                .apply(document("comments-block", requestPreprocessor(), responsePreprocessor(),
                        requestHeaders(
                                headerWithName(HeaderAttribute.X_AUTH_KEY).description(
                                        "Authentication key for the current user")),
                        queryParameters(
                                parameterWithName("commentId").description(
                                        "Comment identifier to block"))));

    }
}

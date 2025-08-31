package com.videoservice.manager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoservice.manager.CommentUseCase;
import com.videoservice.manager.LoadUserPort;
import com.videoservice.manager.UserSessionPort;
import com.videoservice.manager.attribute.HeaderAttribute;
import com.videoservice.manager.comment.CommentResponse;
import com.videoservice.manager.domain.comment.CommentFixtures;
import com.videoservice.manager.domain.user.UserFixtures;
import com.videoservice.manager.dto.CommentRequest;
import com.videoservice.manager.exception.BadRequestException;
import com.videoservice.manager.exception.DomainNotFoundException;
import com.videoservice.manager.exception.ForbiddenRequestException;
import com.videoservice.manager.user.User;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentApiController.class)
class CommentApiControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentUseCase commentUseCase;
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
    @DisplayName("POST /api/v1/comments")
    class CreateComment {

        @Test
        @DisplayName("200 OK, 생성된 id를 반환")
        void testCreateComment() throws Exception {
            // given
            var request = new CommentRequest("channelId", "videoId", null, "comment");
            given(commentUseCase.createComment(any(), any())).willReturn(
                    CommentFixtures.stub("commentId"));

            // when
            mockMvc
                    .perform(
                            post("/api/v1/comments")
                                    .header(HeaderAttribute.X_AUTH_KEY, authKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.id").value("commentId")
                    );
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/comments")
    class UpdateComment {

        @Test
        @DisplayName("200 OK, 변경된 댓글의 id를 반환")
        void testUpdateCommentThenOk() throws Exception {
            // given
            var commentId = "commentId";
            var request = new CommentRequest("channelId", "videoId", null, "new comment");
            given(commentUseCase.updateComment(any(), any(), any())).willReturn(
                    CommentFixtures.stub(commentId));

            // when
            mockMvc
                    .perform(
                            put("/api/v1/comments/{commentId}", commentId)
                                    .header(HeaderAttribute.X_AUTH_KEY, authKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.id").value(commentId)
                    );
        }

        @Test
        @DisplayName("400 BadRequest, 댓글 meta 정보가 다를 경우 수정 실패")
        void testGivenInvalidMetaDataWhenUpdateCommentThenBadRequest() throws Exception {
            // given
            var commentId = "commentId";
            var request = new CommentRequest("otherChannelId", "otherVideoId", null, "new comment");
            given(commentUseCase.updateComment(any(), any(), any()))
                    .willThrow(new BadRequestException("Request metadata is invalid."));

            // when
            mockMvc
                    .perform(
                            put("/api/v1/comments/{commentId}", commentId)
                                    .header(HeaderAttribute.X_AUTH_KEY, authKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpectAll(
                            status().isBadRequest(),
                            jsonPath("$.type").value("badRequest")
                    );
        }

        @Test
        @DisplayName("403 Forbidden, 댓글 작성자와 API 호출자가 다름 수정 실패")
        void testGivenOtherAuthorWhenUpdateCommentThenForbidden() throws Exception {
            // given
            var commentId = "commentId";
            var otherAuthKey = UUID.randomUUID().toString();
            var request = new CommentRequest("channelId", "videoId", null, "new comment");
            given(commentUseCase.updateComment(any(), any(), any()))
                    .willThrow(new ForbiddenRequestException(
                            "Request might not be properly authorized."));

            // when
            mockMvc
                    .perform(
                            put("/api/v1/comments/{commentId}", commentId)
                                    .header(HeaderAttribute.X_AUTH_KEY, otherAuthKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpectAll(
                            status().isForbidden(),
                            jsonPath("$.type").value("forbidden")
                    );
        }

        @Test
        @DisplayName("404 Not Found, 존재하지 않는 댓글 수정 실패")
        void testGivenNoCommentWhenUpdateCommentThenNotFound() throws Exception {
            // given
            var commentId = "commentId";
            var request = new CommentRequest("channelId", "videoId", null, "new comment");
            given(commentUseCase.updateComment(any(), any(), any()))
                    .willThrow(new DomainNotFoundException("Comment Not Found."));

            // when
            mockMvc
                    .perform(
                            put("/api/v1/comments/{commentId}", commentId)
                                    .header(HeaderAttribute.X_AUTH_KEY, authKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.type").value("notFound")
                    );
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/comments/{commentId}")
    class DeleteComment {

        @Test
        @DisplayName("200 Ok, 해당 댓글을 삭제")
        void testGivenCommentThenDeleteComment() throws Exception {
            var commentId = "commentId";

            mockMvc
                    .perform(
                            delete("/api/v1/comments/{commentId}", commentId)
                                    .header(HeaderAttribute.X_AUTH_KEY, authKey)
                    )
                    .andExpect(
                            status().isOk()
                    );

            verify(commentUseCase).deleteComment(commentId, user);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/comments?commentId={commentId}")
    class GetComment {

        @Test
        @DisplayName("200 Ok, 해당 댓글 반환")
        void whenExistThenReturnComment() throws Exception {
            var commentId = "commentId";
            var commentResponse = CommentResponse.builder()
                    .id(commentId)
                    .channelId("channelId")
                    .videoId("videoId")
                    .text("comment")
                    .publishedAt(LocalDateTime.now())
                    .authorId("user")
                    .authorName("user name")
                    .authorProfileImageUrl("https://example.com/profile.jpg")
                    .likeCount(100L)
                    .build();
            given(commentUseCase.getComment(any())).willReturn(commentResponse);

            mockMvc
                    .perform(
                            get("/api/v1/comments?commentId={commentId}", commentId)
                    )
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.id").value(commentId),
                            jsonPath("$.videoId").value("videoId"),
                            jsonPath("$.text").value("comment"),
                            jsonPath("$.authorId").value("user"),
                            jsonPath("$.authorName").value("user name"),
                            jsonPath("$.authorProfileImageUrl").value(
                                    "https://example.com/profile.jpg"),
                            jsonPath("$.likeCount").value(100L)
                    );
        }

        @Test
        @DisplayName("해당 댓글이 없으면 400 Not Found")
        void testGivenCommentNotExistThenNotFound() throws Exception {
            var commentId = "commentId";
            given(commentUseCase.getComment(any())).willThrow(new DomainNotFoundException(""));

            mockMvc
                    .perform(
                            get("/api/v1/comments?commentId={commentId}", commentId)
                    )
                    .andExpect(
                            status().isNotFound()
                    );
        }
    }

    @Nested
    @DisplayName("GET /api/v1/comments/list")
    class ListComment {

        @Test
        @DisplayName("조회하는 회원 정보가 없는 시간 순 정렬")
        void testGivenWithoutUserListCommentsByPublishedAt() throws Exception {
            var videoId = "videoId";
            var order = "time";
            var offset = LocalDateTime.now();
            var maxSize = 10;
            given(commentUseCase.listComments(any(), any(), any(), any(), any())).willReturn(
                    Collections.emptyList());
            mockMvc
                    .perform(
                            get("/api/v1/comments/list?videoId={videoId}&order={order}&offset={offset}&maxSize={maxSize}",
                                    videoId, order, offset, maxSize)
                    )
                    .andExpect(
                            status().isOk()
                    )
                    .andDo(
                            print()
                    );

            verify(commentUseCase).listComments(null, videoId, order, offset.toString(), maxSize);
        }

        @Test
        @DisplayName("조회하는 회원 정보가 있는 시간 순 정렬")
        void testGivenUserListCommentsByPublishedAt() throws Exception {
            var videoId = "videoId";
            var order = "time";
            var offset = LocalDateTime.now();
            var maxSize = 10;
            given(commentUseCase.listComments(any(), any(), any(), any(), any())).willReturn(
                    Collections.emptyList());
            mockMvc
                    .perform(
                            get("/api/v1/comments/list?videoId={videoId}&order={order}&offset={offset}&maxSize={maxSize}",
                                    videoId, order, offset, maxSize)
                                    .header(HeaderAttribute.X_AUTH_KEY, authKey)
                    )
                    .andExpect(
                            status().isOk()
                    )
                    .andDo(
                            print()
                    );

            verify(commentUseCase).listComments(user, videoId, order, offset.toString(), maxSize);
        }
    }
}
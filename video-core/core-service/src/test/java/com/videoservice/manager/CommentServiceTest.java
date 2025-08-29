package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.videoservice.manager.command.CommentCommand;
import com.videoservice.manager.comment.Comment;
import com.videoservice.manager.domain.comment.CommentFixtures;
import com.videoservice.manager.domain.user.UserFixtures;
import com.videoservice.manager.exception.BadRequestException;
import com.videoservice.manager.exception.DomainNotFoundException;
import com.videoservice.manager.exception.ForbiddenRequestException;
import com.videoservice.manager.user.User;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CommentServiceTest {
    private CommentService sut;

    private final CommentPort commentPort = mock(CommentPort.class);
    private final LoadUserPort loadUserPort = mock(LoadUserPort.class);
    private final CommentLikePort commentLikePort = mock(CommentLikePort.class);
    private final CommentBlockPort commentBlockPort = mock(CommentBlockPort.class);

    @BeforeEach
    void setUp() {
        sut = new CommentService(commentPort, loadUserPort, commentLikePort, commentBlockPort);
    }

    @Test
    @DisplayName("댓글 생성")
    void testCreateComment() {
        // given
        var user = UserFixtures.stub();
        var commentCommand = new CommentCommand("channelId", "videoId", null, "comment");
        // when
        sut.createComment(user, commentCommand);
        // then
        var argumentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentPort).saveComment(argumentCaptor.capture());
        then(argumentCaptor.getValue())
                .hasFieldOrPropertyWithValue("authorId", user.getId())
                .hasFieldOrPropertyWithValue("channelId", commentCommand.channelId())
                .hasFieldOrPropertyWithValue("videoId", commentCommand.videoId())
                .hasFieldOrPropertyWithValue("text", commentCommand.text());
    }

    @Nested
    @DisplayName("댓글 수정")
    class UpdateComment {
        @Test
        @DisplayName("댓글 수정 후 수정된 댓글 반환")
        void updateCommentThenReturnUpdatedComment() {
            var commentId = "commentId";
            var user = UserFixtures.stub();
            var comment = CommentFixtures.stub(commentId);
            var commentCommand = new CommentCommand("channelId", "videoId", null, "new comment");
            given(commentPort.loadComment(any())).willReturn(Optional.of(comment));
            given(commentPort.saveComment(any())).willAnswer(arg -> arg.getArgument(0));

            var result = sut.updateComment(commentId, user, commentCommand);

            then(result.getText()).isEqualTo("new comment");
        }

        @Test
        @DisplayName("댓글 channelId, videoId 가 다르면 BadRequestException throw")
        void givenOtherMetaDataThenBadRequestException() {
            var commentId = "commentId";
            var user = UserFixtures.stub();
            var commentCommand = new CommentCommand("otherChannelId", "otherVideoId", null, "new comment");
            given(commentPort.loadComment(any())).willReturn(Optional.of(CommentFixtures.stub(commentId)));

            thenThrownBy(() -> sut.updateComment(commentId, user, commentCommand))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("댓글 수정 user와 댓글 author 가 다르면 ForbiddenRequestException throw")
        void givenOtherUserThenForbiddenRequestException() {
            var commentId = "commentId";
            var otherUser = UserFixtures.stub("otherUser");
            var commentCommand = new CommentCommand("channelId", "videoId", null, "new comment");
            given(commentPort.loadComment(any())).willReturn(Optional.of(CommentFixtures.stub(commentId)));

            thenThrownBy(() -> sut.updateComment(commentId, otherUser, commentCommand))
                    .isInstanceOf(ForbiddenRequestException.class);
        }

        @Test
        @DisplayName("존재하지 않는 댓글이면 DomainNotFoundException throw")
        void givenNoCommentThenDomainNotFoundException() {
            var commentId = "commentId";
            var user = UserFixtures.stub();
            var commentCommand = new CommentCommand("channelId", "videoId", null, "new comment");
            given(commentPort.loadComment(any())).willReturn(Optional.empty());

            thenThrownBy(() -> sut.updateComment(commentId, user, commentCommand))
                    .isInstanceOf(DomainNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {
        @Test
        @DisplayName("댓글을 삭제")
        void givenCommentIdThenDeleteComment() {
            var commentId = "commentId";
            var user = UserFixtures.stub();
            var comment = CommentFixtures.stub(commentId);
            given(commentPort.loadComment(any())).willReturn(Optional.of(comment));

            sut.deleteComment(commentId, user);

            verify(commentPort).deleteComment(commentId);
        }

        @Test
        @DisplayName("댓글 삭제 user와 댓글 author 가 다르면 ForbiddenRequestException throw")
        void givenOtherUserThrowForbiddenRequestException() {
            var commentId = "commentId";
            var otherUser = UserFixtures.stub("otherUser");
            given(commentPort.loadComment(any())).willReturn(Optional.of(CommentFixtures.stub(commentId)));

            thenThrownBy(() -> sut.deleteComment(commentId, otherUser))
                    .isInstanceOf(ForbiddenRequestException.class);
        }

        @Test
        @DisplayName("존재하지 않는 댓글이면 DomainNotFoundException throw")
        void givenNoCommentThenDomainNotFoundException() {
            var commentId = "commentId";
            var user = UserFixtures.stub();
            given(commentPort.loadComment(any())).willReturn(Optional.empty());

            thenThrownBy(() -> sut.deleteComment(commentId, user))
                    .isInstanceOf(DomainNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("댓글 한개 조회")
    class GetComment {
        @Test
        void givenCommentIdThenReturnComment() {
            var comment = CommentFixtures.stub("commentId");
            var author = User.builder().id(comment.getAuthorId()).name("author").profileImageUrl("https://example.com/profile.jpg").build();
            given(commentPort.loadComment(any())).willReturn(Optional.of(comment));
            given(loadUserPort.loadUser(any())).willReturn(Optional.of(author));
            given(commentLikePort.getCommentLikeCount(any())).willReturn(20L);

            var result = sut.getComment("commentId");

            // Then
            then(result)
                    .hasFieldOrPropertyWithValue("id", comment.getId())
                    .hasFieldOrPropertyWithValue("videoId", comment.getVideoId())
                    .hasFieldOrPropertyWithValue("text", comment.getText())
                    .hasFieldOrPropertyWithValue("authorId", author.getId())
                    .hasFieldOrPropertyWithValue("authorName", author.getName())
                    .hasFieldOrPropertyWithValue("authorProfileImageUrl", author.getProfileImageUrl())
                    .hasFieldOrPropertyWithValue("likeCount", 20L);
        }
    }

    @Nested
    @DisplayName("댓글 목록")
    class ListComment {
        @Test
        @DisplayName("User 없는 댓글 목록")
        void testListComment() {
            var videoId = "videoId";
            var comments = LongStream.range(1, 6)
                    .mapToObj(l -> Comment.builder()
                            .id(UUID.randomUUID().toString())
                            .channelId("channelId")
                            .videoId(videoId)
                            .text("text " + l)
                            .authorId("user")
                            .publishedAt(LocalDateTime.now())
                            .build()
                    )
                    .toList();
            var author = User.builder().id("user").name("author").profileImageUrl("https://example.com/profile.jpg").build();
            given(commentPort.listComment(any(), any(), any(), any())).willReturn(comments);
            given(loadUserPort.loadUser(any())).willReturn(Optional.of(author));
            given(commentLikePort.getCommentLikeCount(any())).willReturn(20L);

            var result = sut.listComments(videoId, "time", LocalDateTime.now().toString(), 5);

            // Then
            then(result)
                    .hasSize(5);
            verify(loadUserPort, times(5)).loadUser(any());
            verify(commentLikePort, times(5)).getCommentLikeCount(any());
        }

        @Test
        @DisplayName("User 있는 댓글 목록")
        void testGivenUserListComment() {
            var videoId = "videoId";
            var user = UserFixtures.stub();
            var comments = LongStream.range(1, 6)
                    .mapToObj(l -> Comment.builder()
                            .id(UUID.randomUUID().toString())
                            .channelId("channelId")
                            .videoId(videoId)
                            .text("text " + l)
                            .authorId("user")
                            .publishedAt(LocalDateTime.now())
                            .build()
                    )
                    .toList();
            var blockedComments = Set.of(comments.get(0).getId());
            var author = User.builder().id("user").name("author").profileImageUrl("https://example.com/profile.jpg").build();
            given(commentPort.listComment(any(), any(), any(), any())).willReturn(comments);
            given(commentBlockPort.getUserCommentBlocks(any())).willReturn(blockedComments);
            given(loadUserPort.loadUser(any())).willReturn(Optional.of(author));
            given(commentLikePort.getCommentLikeCount(any())).willReturn(20L);

            var result = sut.listComments(user, videoId, "time", LocalDateTime.now().toString(), 5);

            // Then
            then(result)
                    .hasSize(4);
            verify(loadUserPort, times(4)).loadUser(any());
            verify(commentLikePort, times(4)).getCommentLikeCount(any());
        }
    }

    @Nested
    @DisplayName("대댓글 목록")
    class ListReply {
        @Test
        void testListReply() {
            var parentId = "parentId";
            var comments = LongStream.range(1, 6)
                    .mapToObj(l -> Comment.builder()
                            .id(UUID.randomUUID().toString())
                            .channelId("channelId")
                            .videoId("videoId")
                            .parentId(parentId)
                            .text("text " + l)
                            .authorId("user")
                            .publishedAt(LocalDateTime.now())
                            .build()
                    )
                    .toList();
            var author = User.builder().id("user").name("author").profileImageUrl("https://example.com/profile.jpg").build();
            given(commentPort.listReply(any(), any(), any())).willReturn(comments);
            given(loadUserPort.loadUser(any())).willReturn(Optional.of(author));
            given(commentLikePort.getCommentLikeCount(any())).willReturn(20L);

            var result = sut.listReplies(parentId, LocalDateTime.now().toString(), 5);

            // Then
            then(result)
                    .hasSize(5);
            verify(loadUserPort, times(5)).loadUser(any());
            verify(commentLikePort, times(5)).getCommentLikeCount(any());
        }
    }
}
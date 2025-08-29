package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.videoservice.manager.comment.Comment;
import com.videoservice.manager.domain.comment.CommentFixtures;
import com.videoservice.manager.mongo.comment.CommentDocument;
import com.videoservice.manager.mongo.comment.CommentMongoRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class CommentPersistenceAdapterTest {
    private CommentPersistenceAdapter sut;

    private final CommentMongoRepository commentMongoRepository = mock(CommentMongoRepository.class);
    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);

    @BeforeEach
    void setUp() {
        sut = new CommentPersistenceAdapter(commentMongoRepository,stringRedisTemplate);
    }

    @Test
    void testSaveComment() {
        var comment = CommentFixtures.stub("commentId");
        given(commentMongoRepository.save(any())).willAnswer(arg -> arg.getArgument(0));

        var result = sut.saveComment(comment);

        then(result)
                .hasFieldOrPropertyWithValue("id", "commentId");
    }

    @Test
    void testDeleteComment() {
        var commentId = "commentId";

        sut.deleteComment(commentId);

        verify(commentMongoRepository).deleteById(commentId);
    }

    @Test
    void testLoadComment() {
        var commentId = "commentId";
        var comment = CommentFixtures.stub(commentId);
        given(commentMongoRepository.findById(any())).willReturn(Optional.of(CommentDocument.from(comment)));

        var result = sut.loadComment(commentId);
        then(result)
                .isPresent()
                .hasValueSatisfying(c ->
                        then(c)
                                .hasFieldOrPropertyWithValue("id", commentId)
                                .hasFieldOrPropertyWithValue("text", comment.getText())
                );
    }

    @Nested
    @DisplayName("댓글 목록")
    class ListComment {
        @Test
        @DisplayName("작성 시간 역순으로 maxSize 만큼 목록 반환")
        void testGivenPublishedAtDescThenReturnList() {
            var videoId = "videoId";
            var list = LongStream.range(1, 6)
                    .mapToObj(i -> commentBuilder(videoId, LocalDateTime.now()))
                    .toList();
            given(commentMongoRepository.findAllByVideoIdAndParentIdAndPublishedAtLessThanEqualOrderByPublishedAtDesc(any(), any(), any(), any()))
                    .willReturn(list);
            var result = sut.listComment(videoId, "time", "2024-05-01T12:34:56.789", 5);

            then(result)
                    .hasSize(5);
        }
    }

    @Nested
    @DisplayName("대댓글 목록")
    class ListReply {
        @Test
        @DisplayName("작성 시간 역순으로 maxSize 만큼 목록 반환")
        void testGivenPublishedAtDescThenReturnList() {
            var parentId = "parentId";
            var list = LongStream.range(1, 6)
                    .mapToObj(i -> replyBuilder(parentId, LocalDateTime.now()))
                    .toList();
            given(commentMongoRepository.findAllByParentIdAndPublishedAtLessThanEqualOrderByPublishedAtDesc(any(), any(), any()))
                    .willReturn(list);
            var result = sut.listReply(parentId, "2024-05-01T12:34:56.789", 5);

            then(result)
                    .hasSize(5);
        }
    }

    @Nested
    @DisplayName("고정 댓글")
    class PinnedComment {
        @Test
        void givenPinnedCommentThenReturnOptionalComment() {
            var videoId = "videoId";
            var commentId = "commentId";
            given(stringRedisTemplate.opsForValue().get(any())).willReturn(commentId);
            given(commentMongoRepository.findById(any())).willReturn(
                    Optional.of(commentBuilder(videoId, LocalDateTime.now())));

            var result = sut.getPinnedComment("videoId");

            then(result)
                    .isPresent();
        }

        @Test
        void givenNoPinnedCommentThenReturnEmptyOptional() {
            given(stringRedisTemplate.opsForValue().get(any())).willReturn(null);

            var result = sut.getPinnedComment("videoId");

            then(result)
                    .isNotPresent();
        }
    }

    private CommentDocument commentBuilder(String videoId, LocalDateTime publishedAt) {
        var id = UUID.randomUUID().toString();
        return CommentDocument.from(
                Comment.builder()
                        .id(id)
                        .channelId("channelId")
                        .videoId(videoId)
                        .parentId(null)
                        .text("text " + id)
                        .authorId("user")
                        .publishedAt(publishedAt)
                        .build()
        );
    }

    private CommentDocument replyBuilder(String parentId, LocalDateTime publishedAt) {
        var id = UUID.randomUUID().toString();
        return CommentDocument.from(
                Comment.builder()
                        .id(id)
                        .channelId("channelId")
                        .videoId("videoId")
                        .parentId(parentId)
                        .text("text " + id)
                        .authorId("user")
                        .publishedAt(publishedAt)
                        .build()
        );
    }
}
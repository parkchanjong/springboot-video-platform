package com.videoservice.manager;

import com.videoservice.manager.comment.Comment;
import com.videoservice.manager.redis.common.RedisKeyGenerator;
import com.videoservice.manager.mongo.comment.CommentDocument;
import com.videoservice.manager.mongo.comment.CommentMongoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CommentPersistenceAdapter implements CommentPort {

    private final CommentMongoRepository commentMongoRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public CommentPersistenceAdapter(CommentMongoRepository commentMongoRepository,
            StringRedisTemplate stringRedisTemplate) {
        this.commentMongoRepository = commentMongoRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Comment saveComment(Comment comment) {
        var commentDocument = CommentDocument.from(comment);

        return commentMongoRepository.save(commentDocument)
                .toDomain();
    }

    @Override
    public void deleteComment(String commentId) {
        commentMongoRepository.deleteById(commentId);
    }

    @Override
    public Optional<Comment> loadComment(String commentId) {
        return commentMongoRepository.findById(commentId)
                .map(CommentDocument::toDomain);
    }

    @Override
    public List<Comment> listComment(String videoId, String order, String offset, Integer maxSize) {
        return commentMongoRepository.findAllByVideoIdAndParentIdAndPublishedAtLessThanEqualOrderByPublishedAtDesc(
                        videoId, null, LocalDateTime.parse(offset), Limit.of(maxSize))
                .stream()
                .map(CommentDocument::toDomain)
                .toList();
    }

    @Override
    public List<Comment> listReply(String parentId, String offset, Integer maxSize) {
        return commentMongoRepository.findAllByParentIdAndPublishedAtLessThanEqualOrderByPublishedAtDesc(
                        parentId, LocalDateTime.parse(offset), Limit.of(maxSize))
                .stream()
                .map(CommentDocument::toDomain)
                .toList();
    }

    @Override
    public Optional<Comment> getPinnedComment(String videoId) {
        var commentId = stringRedisTemplate.opsForValue()
                .get(RedisKeyGenerator.getPinnedCommentKey(videoId));
        if (commentId == null) {
            return Optional.empty();
        }

        return commentMongoRepository.findById(commentId)
                .map(CommentDocument::toDomain);
    }
}

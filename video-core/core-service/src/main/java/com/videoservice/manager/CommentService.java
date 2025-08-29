package com.videoservice.manager;

import com.videoservice.manager.command.CommentCommand;
import com.videoservice.manager.comment.Comment;
import com.videoservice.manager.comment.CommentResponse;
import com.videoservice.manager.exception.BadRequestException;
import com.videoservice.manager.exception.DomainNotFoundException;
import com.videoservice.manager.exception.ForbiddenRequestException;
import com.videoservice.manager.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CommentService implements CommentUseCase {
    private final CommentPort commentPort;
    private final LoadUserPort loadUserPort;
    private final CommentLikePort commentLikePort;
    private final CommentBlockPort commentBlockPort;

    public CommentService(
            CommentPort commentPort,
            LoadUserPort loadUserPort, CommentLikePort commentLikePort, CommentBlockPort commentBlockPort) {
        this.commentPort = commentPort;
        this.loadUserPort = loadUserPort;
        this.commentLikePort = commentLikePort;
        this.commentBlockPort = commentBlockPort;
    }

    @Override
    public Comment createComment(User user, CommentCommand commentCommand) {
        var comment = Comment.builder()
                .id(UUID.randomUUID().toString())
                .channelId(commentCommand.channelId())
                .videoId(commentCommand.videoId())
                .parentId(commentCommand.parentId())
                .text(commentCommand.text())
                .authorId(user.getId())
                .publishedAt(LocalDateTime.now())
                .build();

        return commentPort.saveComment(comment);
    }

    @Override
    public Comment updateComment(String commentId, User user, CommentCommand commentCommand) {
        var comment = commentPort.loadComment(commentId)
                .orElseThrow(() -> new DomainNotFoundException("Comment Not Found."));

        if (!Objects.equals(comment.getAuthorId(), user.getId())) {
            throw new ForbiddenRequestException("The request might not be properly authorized.");
        }
        if (!equalMetaData(comment, commentCommand)) {
            throw new BadRequestException("Request metadata is invalid.");
        }

        comment.updateText(commentCommand.text());

        return commentPort.saveComment(comment);
    }

    @Override
    public void deleteComment(String commentId, User user) {
        var comment = commentPort.loadComment(commentId)
                .orElseThrow(() -> new DomainNotFoundException("Comment Not Found."));

        if (!Objects.equals(comment.getAuthorId(), user.getId())) {
            throw new ForbiddenRequestException("The request might not be properly authorized.");
        }

        commentPort.deleteComment(commentId);
    }

    @Override
    public CommentResponse getComment(String commentId) {
        var comment = commentPort.loadComment(commentId)
                .orElseThrow(() -> new DomainNotFoundException("Comment Not Found."));

        return buildComment(comment);
    }

    @Override
    public List<CommentResponse> listComments(String videoId, String order, String offset, Integer maxSize) {
        var list = commentPort.listComment(videoId, order, offset, maxSize).stream()
                .map(comment -> {
                    var user = loadUserPort.loadUser(comment.getAuthorId())
                            .orElse(User.defaultUser(comment.getAuthorId()));
                    var commentLikeCount = commentLikePort.getCommentLikeCount(comment.getId());
                    var replies = commentPort.listReply(comment.getId(), offset, 100).stream()
                            .map(this::buildComment)
                            .toList();
                    return CommentResponse.from(comment, user, commentLikeCount, replies);
                })
                .collect(Collectors.toList());
        commentPort.getPinnedComment(videoId)
                .ifPresent(pinnedComment -> {
                    var pinnedCommentResponse = buildComment(pinnedComment);
                    list.add(0, pinnedCommentResponse);
                });

        return list;
    }

    @Override
    public List<CommentResponse> listComments(User user, String videoId, String order, String offset, Integer maxSize) {
        var commentBlocks = commentBlockPort.getUserCommentBlocks(user.getId());

        return commentPort.listComment(videoId, order, offset, maxSize).stream()
                .filter(comment -> !commentBlocks.contains(comment.getId()))
                .map(comment -> {
                    var author = loadUserPort.loadUser(comment.getAuthorId())
                            .orElse(User.defaultUser(comment.getAuthorId()));
                    var commentLikeCount = commentLikePort.getCommentLikeCount(comment.getId());
                    var replies = commentPort.listReply(comment.getId(), offset, 100).stream()
                            .map(this::buildComment)
                            .toList();
                    return CommentResponse.from(comment, author, commentLikeCount, replies);
                })
                .toList();
    }

    @Override
    public List<CommentResponse> listReplies(String parentId, String offset, Integer maxSize) {
        return commentPort.listReply(parentId, offset, maxSize).stream()
                .map(this::buildComment)
                .collect(Collectors.toList());
    }

    private boolean equalMetaData(Comment comment, CommentCommand commentCommand) {
        return Objects.equals(comment.getChannelId(), commentCommand.channelId()) &&
                Objects.equals(comment.getVideoId(), commentCommand.videoId()) &&
                Objects.equals(comment.getParentId(), commentCommand.parentId());
    }

    private CommentResponse buildComment(Comment comment) {
        var user = loadUserPort.loadUser(comment.getAuthorId())
                .orElse(User.defaultUser(comment.getAuthorId()));
        var commentLikeCount = commentLikePort.getCommentLikeCount(comment.getId());
        return CommentResponse.from(comment, user, commentLikeCount);
    }
}

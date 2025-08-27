package com.videoservice.manager.comment;

import com.videoservice.manager.user.User;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class CommentResponse {
    private String id;
    private String channelId;
    private String videoId;
    private String parentId;
    private String text;
    private LocalDateTime publishedAt;
    private String authorId;
    private String authorName;
    private String authorProfileImageUrl;
    private Long likeCount;
    private Integer replyCount;
    private List<CommentResponse> replies;

    public static CommentResponse from(Comment comment, User author, Long likeCount) {
        return CommentResponse.builder()
                .id(comment.getId())
                .channelId(comment.getChannelId())
                .videoId(comment.getVideoId())
                .parentId(comment.getParentId())
                .text(comment.getText())
                .publishedAt(comment.getPublishedAt())
                .authorId(author.getId())
                .authorName(author.getName())
                .authorProfileImageUrl(author.getProfileImageUrl())
                .likeCount(likeCount)
                .replyCount(0)
                .replies(Collections.emptyList())
                .build();
    }

    public static CommentResponse from(Comment comment, User author, Long likeCount, List<CommentResponse> replies) {
        return CommentResponse.builder()
                .id(comment.getId())
                .channelId(comment.getChannelId())
                .videoId(comment.getVideoId())
                .parentId(comment.getParentId())
                .text(comment.getText())
                .publishedAt(comment.getPublishedAt())
                .authorId(author.getId())
                .authorName(author.getName())
                .authorProfileImageUrl(author.getProfileImageUrl())
                .likeCount(likeCount)
                .replyCount(replies.size())
                .replies(replies)
                .build();
    }
}

package com.videoservice.manager.domain.comment;

import com.videoservice.manager.comment.Comment;
import com.videoservice.manager.comment.CommentResponse;
import java.time.LocalDateTime;
import java.util.List;

public class CommentResponseFixtures {
    public static CommentResponse stub(String id) {
        return CommentResponse.builder()
                .id(id)
                .channelId("channelId")
                .videoId("videoId")
                .parentId("parent-0")
                .text("comment")
                .publishedAt(LocalDateTime.now())
                .authorId("user")
                .authorName("user name")
                .authorProfileImageUrl("https://example.com/profile.jpg")
                .likeCount(1L)
                .replyCount(1)
                .replies(List.of(CommentResponseFixtures.stub("reply-1")))
                .build();
    }
}

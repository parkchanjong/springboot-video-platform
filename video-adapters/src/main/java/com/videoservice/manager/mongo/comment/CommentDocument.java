package com.videoservice.manager.mongo.comment;

import com.videoservice.manager.comment.Comment;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document("comment")
@AllArgsConstructor
@Getter
public class CommentDocument {
    @Id
    private String id;
    private String channelId;
    @Indexed
    private String videoId;
    @Indexed
    private String parentId;
    private String authorId;
    private String text;
    @Indexed
    private LocalDateTime publishedAt;

    public static CommentDocument from(Comment comment) {
        return new CommentDocument(
                comment.getId(),
                comment.getChannelId(),
                comment.getVideoId(),
                comment.getParentId(),
                comment.getAuthorId(),
                comment.getText(),
                comment.getPublishedAt()
        );
    }

    public Comment toDomain() {
        return Comment.builder()
                .id(this.getId())
                .channelId(this.getChannelId())
                .videoId(this.getVideoId())
                .parentId(this.getParentId())
                .text(this.getText())
                .authorId(this.authorId)
                .publishedAt(this.getPublishedAt())
                .build();
    }
}
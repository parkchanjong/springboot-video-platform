package com.videoservice.manager.comment;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class Comment {
    private String id;
    private String channelId;
    private String videoId;
    private String parentId;
    private String authorId;
    private String text;
    private LocalDateTime publishedAt;

    public void updateText(String text) {
        this.text = text;
    }
}

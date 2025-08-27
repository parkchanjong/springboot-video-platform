package com.videoservice.manager.video;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Video implements Serializable {
    private String id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String fileUrl;
    private String channelId;
    private Long viewCount;
    private Long likeCount;
    private LocalDateTime publishedAt;

    public void bindCount(long viewCount, Long likeCount) {
        this.viewCount = viewCount;
        this.likeCount = likeCount;
    }
}
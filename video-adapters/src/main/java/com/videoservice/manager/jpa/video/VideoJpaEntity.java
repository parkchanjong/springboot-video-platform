package com.videoservice.manager.jpa.video;

import com.videoservice.manager.video.Video;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "video")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class VideoJpaEntity {
    @Id
    private String id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String fileUrl;
    private String channelId;
    private Long viewCount = 0L;
    private LocalDateTime publishedAt;

    public Video toDomain() {
        return Video.builder()
                .id(this.getId())
                .title(this.getTitle())
                .description(this.getDescription())
                .thumbnailUrl(this.getThumbnailUrl())
                .fileUrl(this.getFileUrl())
                .channelId(this.channelId)
                .publishedAt(this.getPublishedAt())
                .build();
    }

    public static VideoJpaEntity from(Video video) {
        return new VideoJpaEntity(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getThumbnailUrl(),
                video.getFileUrl(),
                video.getChannelId(),
                video.getViewCount(),
                video.getPublishedAt()
        );
    }

    public void updateViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }
}

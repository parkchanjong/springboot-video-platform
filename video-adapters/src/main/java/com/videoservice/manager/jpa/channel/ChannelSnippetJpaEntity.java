package com.videoservice.manager.jpa.channel;

import com.videoservice.manager.channel.ChannelSnippet;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ChannelSnippetJpaEntity {
    private String title;
    private String description;
    private String thumbnailUrl;
    private LocalDateTime publishedAt;

    public static ChannelSnippetJpaEntity from(ChannelSnippet channelSnippet) {
        return new ChannelSnippetJpaEntity(channelSnippet.getTitle(), channelSnippet.getDescription(), channelSnippet.getThumbnailUrl(), channelSnippet.getPublishedAt());
    }

    public ChannelSnippet toDomain() {
        return ChannelSnippet.builder()
                .title(this.getTitle())
                .description(this.getDescription())
                .thumbnailUrl(this.getThumbnailUrl())
                .publishedAt(this.getPublishedAt())
                .build();
    }
}

package com.videoservice.manager.jpa.channel;

import com.videoservice.manager.channel.Channel;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "channel")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ChannelJpaEntity {
    @Id
    private String id;

    @Embedded
    private ChannelSnippetJpaEntity snippet;

    @Embedded
    private ChannelStatisticsJpaEntity statistics;

    private String contentOwnerId;

    public Channel toDomain() {
        return Channel.builder()
                .id(this.getId())
                .snippet(this.getSnippet().toDomain())
                .statistics(this.getStatistics().toDomain())
                .contentOwnerId(this.getContentOwnerId())
                .build();
    }

    public static ChannelJpaEntity from(Channel channel) {
        return new ChannelJpaEntity(
                channel.getId(),
                ChannelSnippetJpaEntity.from(channel.getSnippet()),
                ChannelStatisticsJpaEntity.from(channel.getStatistics()),
                channel.getContentOwnerId()
        );
    }
}

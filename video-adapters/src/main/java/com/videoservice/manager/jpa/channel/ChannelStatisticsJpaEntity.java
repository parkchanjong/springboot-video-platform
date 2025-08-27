package com.videoservice.manager.jpa.channel;

import com.videoservice.manager.channel.ChannelStatistics;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ChannelStatisticsJpaEntity {
    private long videoCount;
    private long subscriberCount;
    private long commentCount;

    public static ChannelStatisticsJpaEntity from(ChannelStatistics statistics) {
        return new ChannelStatisticsJpaEntity(statistics.getVideoCount(), statistics.getSubscriberCount(), statistics.getCommentCount());
    }

    public ChannelStatistics toDomain() {
        return ChannelStatistics.builder()
                .videoCount(this.getVideoCount())
                .subscriberCount(this.getSubscriberCount())
                .commentCount(this.getCommentCount())
                .build();
    }
}

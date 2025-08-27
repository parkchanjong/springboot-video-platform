package com.videoservice.manager.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class ChannelStatistics {
    private long videoCount;
    private long subscriberCount;
    private long commentCount;

    public static ChannelStatistics getDefaultStatistics() {
        return ChannelStatistics.builder()
                .subscriberCount(0)
                .videoCount(0)
                .commentCount(0)
                .build();
    }

    public void updateVideoCount(Long videoCount) {
        this.videoCount = videoCount;
    }
}

package com.videoservice.manager.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class Channel {
    private String id;
    private ChannelSnippet snippet;
    private ChannelStatistics statistics;
    private String contentOwnerId;

    public void updateSnippet(String title, String description, String thumbnailUrl) {
        this.snippet = ChannelSnippet.builder()
                .title(title)
                .description(description)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }
}

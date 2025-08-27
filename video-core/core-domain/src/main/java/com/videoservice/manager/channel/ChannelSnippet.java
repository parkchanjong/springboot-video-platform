package com.videoservice.manager.channel;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class ChannelSnippet {
    private String title;
    private String description;
    private String thumbnailUrl;
    private LocalDateTime publishedAt;
}


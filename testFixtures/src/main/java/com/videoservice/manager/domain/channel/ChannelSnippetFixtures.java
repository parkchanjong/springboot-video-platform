package com.videoservice.manager.domain.channel;

import com.videoservice.manager.channel.ChannelSnippet;
import java.time.LocalDateTime;

public class ChannelSnippetFixtures {
    public static ChannelSnippet stub() {
        return ChannelSnippet.builder()
                .title("title")
                .description("description")
                .thumbnailUrl("https://example.com/thumbnail.jpg")
                .publishedAt(LocalDateTime.now())
                .build();
    }

    public static ChannelSnippet anotherStub() {
        return ChannelSnippet.builder()
                .title("title 2")
                .description("description 2")
                .thumbnailUrl("https://example.com/thumbnail2.jpg")
                .publishedAt(LocalDateTime.now())
                .build();
    }
}

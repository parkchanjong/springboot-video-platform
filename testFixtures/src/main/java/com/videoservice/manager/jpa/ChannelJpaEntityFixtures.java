package com.videoservice.manager.jpa;

import com.videoservice.manager.jpa.channel.ChannelJpaEntity;
import com.videoservice.manager.jpa.channel.ChannelSnippetJpaEntity;
import com.videoservice.manager.jpa.channel.ChannelStatisticsJpaEntity;
import java.time.LocalDateTime;

public class ChannelJpaEntityFixtures {
    public static ChannelJpaEntity stub(String id) {
        return new ChannelJpaEntity(
                id,
                new ChannelSnippetJpaEntity("title", "description", "https://example.com/thumbnail", LocalDateTime.now()),
                new ChannelStatisticsJpaEntity(10L, 10L, 10L),
                "user"
        );
    }
}
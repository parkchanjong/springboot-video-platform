package com.videoservice.manager.domain.channel;

import com.videoservice.manager.channel.Channel;

public class ChannelFixtures {
    public static Channel stub(String id) {
        return Channel.builder()
                .id(id)
                .snippet(ChannelSnippetFixtures.stub())
                .statistics(ChannelStatisticsFixtures.stub())
                .contentOwnerId("user")
                .build();
    }
}

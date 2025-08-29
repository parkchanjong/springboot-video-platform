package com.videoservice.manager;

import com.videoservice.manager.channel.Channel;
import com.videoservice.manager.channel.ChannelSnippet;
import com.videoservice.manager.channel.ChannelStatistics;
import com.videoservice.manager.command.ChannelCommand;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ChannelService implements ChannelUseCase {
    private final LoadChannelPort loadChannelPort;
    private final SaveChannelPort saveChannelPort;

    public ChannelService(LoadChannelPort loadChannelPort, SaveChannelPort saveChannelPort) {
        this.loadChannelPort = loadChannelPort;
        this.saveChannelPort = saveChannelPort;
    }

    @Override
    public Channel createChannel(ChannelCommand channelCommand) {
        var channel = Channel.builder()
                .id(UUID.randomUUID().toString())
                .snippet(
                        ChannelSnippet.builder()
                                .title(channelCommand.snippet().title())
                                .description(channelCommand.snippet().description())
                                .thumbnailUrl(channelCommand.snippet().thumbnailUrl())
                                .publishedAt(LocalDateTime.now())
                                .build()
                )
                .statistics(ChannelStatistics.getDefaultStatistics())
                .contentOwnerId(channelCommand.contentOwnerId())
                .build();

        saveChannelPort.saveChannel(channel);
        return channel;
    }

    @Override
    public Channel updateChannel(String channelId, ChannelCommand channelCommand) {
        var channel = loadChannelPort.loadChannel(channelId).get();
        channel.updateSnippet(channelCommand.snippet().title(),
                channelCommand.snippet().description(),
                channelCommand.snippet().thumbnailUrl());

        saveChannelPort.saveChannel(channel);
        return channel;
    }

    @Override
    public Channel getChannel(String id) {
        return loadChannelPort.loadChannel(id).get();
    }
}

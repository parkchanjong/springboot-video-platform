package com.videoservice.manager;

import com.videoservice.manager.channel.Channel;
import com.videoservice.manager.command.ChannelCommand;

public interface ChannelUseCase {
    Channel createChannel(ChannelCommand channelCommand);
    Channel updateChannel(String channelId, ChannelCommand channelCommand);
    Channel getChannel(String id);
}
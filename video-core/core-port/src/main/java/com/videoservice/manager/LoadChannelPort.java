package com.videoservice.manager;

import com.videoservice.manager.channel.Channel;
import java.util.Optional;

public interface LoadChannelPort {
    Optional<Channel> loadChannel(String id);
}

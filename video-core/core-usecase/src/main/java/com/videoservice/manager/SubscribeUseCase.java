package com.videoservice.manager;

import com.videoservice.manager.channel.Channel;
import java.util.List;

public interface SubscribeUseCase {
    String subscribeChannel(String channelId, String userId);

    void unsubscribeChannel(String subscribeId, String userId);

    List<Channel> listSubscribeChannel(String userId);
}

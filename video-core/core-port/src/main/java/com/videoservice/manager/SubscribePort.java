package com.videoservice.manager;

import com.videoservice.manager.channel.Channel;
import com.videoservice.manager.user.User;
import java.util.List;

public interface SubscribePort {
    String insertSubscribeChannel(Channel channel, User user);

    void deleteSubscribeChannel(String subscribeId);

    List<Channel> listSubscribeChannel(String userId);
}

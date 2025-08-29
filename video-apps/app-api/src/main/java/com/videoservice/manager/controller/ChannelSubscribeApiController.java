package com.videoservice.manager.controller;

import com.videoservice.manager.SubscribeUseCase;
import com.videoservice.manager.channel.Channel;
import com.videoservice.manager.dto.CommandResponse;
import com.videoservice.manager.user.User;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscribe")
public class ChannelSubscribeApiController {
    private final SubscribeUseCase subscribeUseCase;

    public ChannelSubscribeApiController(SubscribeUseCase subscribeUseCase) {
        this.subscribeUseCase = subscribeUseCase;
    }

    @PostMapping
    CommandResponse subscribe(
            User user,
            @RequestParam String channelId
    ) {
        var subscribeId = subscribeUseCase.subscribeChannel(channelId, user.getId());
        return new CommandResponse(subscribeId);
    }

    @DeleteMapping
    void unsubscribe(
            User user,
            @RequestParam String subscribeId
    ) {
        subscribeUseCase.unsubscribeChannel(subscribeId, user.getId());
    }

    @GetMapping("/mine")
    List<Channel> listSubscribeChannelByUser(User user) {
        return subscribeUseCase.listSubscribeChannel(user.getId());
    }
}



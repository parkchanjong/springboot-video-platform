package com.videoservice.manager.command;

public record VideoCommand(
        String title,
        String description,
        String thumbnailUrl,
        String channelId
) {

}

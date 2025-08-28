package com.videoservice.manager.command;

public record CommentCommand(
        String channelId,
        String videoId,
        String parentId,
        String text
) {

}

package com.videoservice.manager.command;

public record ChannelCommand(
        ChannelSnippetCommand snippet,
        String contentOwnerId
) {

    public record ChannelSnippetCommand(
            String title,
            String description,
            String thumbnailUrl
    ) {}

}

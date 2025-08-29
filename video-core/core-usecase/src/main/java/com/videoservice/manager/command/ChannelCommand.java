package com.videoservice.manager.command;

import lombok.Builder;

@Builder
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

package com.videoservice.manager.dto;

import com.videoservice.manager.command.ChannelCommand;
import com.videoservice.manager.command.ChannelCommand.ChannelSnippetCommand;

public record ChannelRequest(
        ChannelSnippetRequest snippet,
        String contentOwnerId
) {

    public record ChannelSnippetRequest(
            String title,
            String description,
            String thumbnailUrl
    ) {

        public ChannelSnippetCommand toSnippetCommand() {
            return new ChannelSnippetCommand(title, description, thumbnailUrl);
        }
    }

    public ChannelCommand toCommand() {
        return new ChannelCommand(snippet.toSnippetCommand(), contentOwnerId);
    }
}

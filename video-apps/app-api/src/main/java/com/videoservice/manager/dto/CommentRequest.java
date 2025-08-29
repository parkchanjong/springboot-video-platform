package com.videoservice.manager.dto;

import com.videoservice.manager.command.CommentCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CommentRequest {
    private String channelId;
    private String videoId;
    private String parentId;
    private String text;

    public CommentCommand toCommand() {
        return new CommentCommand(channelId, videoId, parentId, text);
    }
}

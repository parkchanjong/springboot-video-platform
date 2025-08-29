package com.videoservice.manager.dto;

import com.videoservice.manager.command.VideoCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VideoRequest {
    private String title;
    private String description;
    private String thumbnailUrl;
    private String channelId;

    public VideoCommand toCommand() {
        return new VideoCommand(title, description, thumbnailUrl, channelId);
    }
}

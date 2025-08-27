package com.videoservice.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VideoRequest {
    private String title;
    private String description;
    private String thumbnailUrl;
    private String channelId;
}

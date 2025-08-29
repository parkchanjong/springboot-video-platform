package com.videoservice.manager.domain.video;

import com.videoservice.manager.video.Video;

public class VideoFixtures {
    public static Video stub(String id) {
        return Video.builder()
                .id(id)
                .title("video title")
                .description("video description")
                .thumbnailUrl("https://example.com/thumbnail.jpg")
                .fileUrl("https://example.com/video.mp4")
                .channelId("channelId")
                .viewCount(0L)
                .build();
    }
}
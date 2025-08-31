package com.videoservice.manager.jpa;

import com.videoservice.manager.jpa.video.VideoJpaEntity;
import java.time.LocalDateTime;

public class VideoJpaEntityFixtures {
    public static VideoJpaEntity stub(String id) {
        return new VideoJpaEntity(
                id,
                "title",
                "description",
                "https://example.com/thumbnail.jpg",
                "https://example.com/video.mp4",
                "channelId",
                10L,
                LocalDateTime.now()
        );
    }
}
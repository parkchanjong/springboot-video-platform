package com.videoservice.manager.video;

import static org.assertj.core.api.BDDAssertions.then;

import com.videoservice.manager.domain.video.VideoFixtures;
import org.junit.jupiter.api.Test;

class VideoTest {
    @Test
    void testBindViewCount() {
        var video = VideoFixtures.stub("videoId");
        video.bindCount(100L, 30L);

        then(video.getViewCount()).isEqualTo(100L);
        then(video.getLikeCount()).isEqualTo(30L);
    }
}
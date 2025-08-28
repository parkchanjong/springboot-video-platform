package com.videoservice.manager;

import com.videoservice.manager.video.Video;

public interface SaveVideoPort {
    void saveVideo(Video video);
    void incrementViewCount(String videoId);
    void syncViewCount(String videoId);
}

package com.videoservice.manager;

import com.videoservice.manager.video.Video;
import java.util.List;

public interface LoadVideoPort {
    Video loadVideo(String videoId);
    List<Video> loadVideoByChannel(String channelId);
    Long getViewCount(String videoId);
    List<String> getAllVideoIdsWithViewCount();
}

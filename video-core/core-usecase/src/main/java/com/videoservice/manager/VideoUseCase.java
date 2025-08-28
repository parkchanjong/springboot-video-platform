package com.videoservice.manager;

import com.videoservice.manager.command.VideoCommand;
import com.videoservice.manager.video.Video;
import java.util.List;

public interface VideoUseCase {
    Video getVideo(String videoId);

    List<Video> listVideos(String channelId);

    Video createVideo(VideoCommand videoCommand);

    void increaseViewCount(String videoId);
}
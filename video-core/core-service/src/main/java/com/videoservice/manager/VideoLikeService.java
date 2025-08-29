package com.videoservice.manager;

import org.springframework.stereotype.Service;

@Service
public class VideoLikeService implements VideoLikeUseCase {
    private final VideoLikePort videoLikePort;

    public VideoLikeService(VideoLikePort videoLikePort) {
        this.videoLikePort = videoLikePort;
    }

    @Override
    public Long likeVideo(String videoId, String userId) {
        return videoLikePort.addVideoLike(videoId, userId);
    }

    @Override
    public Long unlikeVideo(String videoId, String userId) {
        return videoLikePort.removeVideoLike(videoId, userId);
    }

    @Override
    public Boolean isLikedVideo(String videoId, String userId) {
        return videoLikePort.isVideoLikeMember(videoId, userId);
    }

    @Override
    public Long getVideoLikeCount(String videoId) {
        return videoLikePort.getVideoLikeCount(videoId);
    }
}

package com.videoservice.manager.controller;

import com.videoservice.manager.VideoLikeUseCase;
import com.videoservice.manager.dto.VideoRateResponse;
import com.videoservice.manager.user.User;
import com.videoservice.manager.video.VideoRate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/videos/rate")
public class VideoRateApiController {

    private final VideoLikeUseCase videoLikeUseCase;

    public VideoRateApiController(VideoLikeUseCase videoLikeUseCase) {
        this.videoLikeUseCase = videoLikeUseCase;
    }

    @PostMapping
    void rateVideo(
            User user,
            @RequestParam String videoId,
            @RequestParam VideoRate rating
    ) {
        switch (rating) {
            case like:
                videoLikeUseCase.likeVideo(videoId, user.getId());
                break;
            case none:
                videoLikeUseCase.unlikeVideo(videoId, user.getId());
                break;
        }
    }

    @GetMapping
    VideoRateResponse getRate(
            User user,
            @RequestParam String videoId
    ) {
        var rate = Boolean.TRUE.equals(videoLikeUseCase.isLikedVideo(videoId, user.getId())) ? VideoRate.like
                : VideoRate.none;
        return new VideoRateResponse(videoId, rate);
    }
}

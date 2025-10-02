package com.videoservice.manager.controller;

import com.videoservice.manager.VideoUseCase;
import com.videoservice.manager.dto.CommandResponse;
import com.videoservice.manager.dto.VideoRequest;
import com.videoservice.manager.video.Video;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoApiController {
    private final VideoUseCase videoUseCase;

    public VideoApiController(VideoUseCase videoUseCase) {
        this.videoUseCase = videoUseCase;
    }

    @GetMapping("/{videoId}")
    public Video getVideo(@PathVariable String videoId) {
        return videoUseCase.getVideo(videoId);
    }

    @GetMapping(params = "channelId")
    public List<Video> listVideo(@RequestParam String channelId) {
        return videoUseCase.listVideos(channelId);
    }

    @PostMapping
    public CommandResponse createVideo(@RequestBody VideoRequest videoRequest) {
        var video = videoUseCase.createVideo(videoRequest.toCommand());
        return new CommandResponse(video.getId());
    }

    @PostMapping("/{videoId}/view")
    public void increaseVideoViewCount(@PathVariable String videoId) {
        videoUseCase.increaseViewCount(videoId);
    }
}

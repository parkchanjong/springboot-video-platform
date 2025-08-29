package com.videoservice.manager.dto;

import com.videoservice.manager.video.VideoRate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VideoRateResponse {
    private String videoId;
    private VideoRate rate;
}

package com.videoservice.manager.external.kakao;

public record KakaoErrorResponse(
        String errorType,
        String message
) {
}
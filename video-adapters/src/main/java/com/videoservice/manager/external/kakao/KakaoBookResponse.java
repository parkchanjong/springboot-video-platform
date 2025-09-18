package com.videoservice.manager.external.kakao;

import java.util.List;

public record KakaoBookResponse(
        List<Document> documents,
        Meta meta
) {
}
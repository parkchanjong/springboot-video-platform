package com.videoservice.manager.external.kakao;

import java.util.List;

public record Document(
        String title,
        List<String> authors,
        String isbn,
        String publisher,
        String datetime
) {
}
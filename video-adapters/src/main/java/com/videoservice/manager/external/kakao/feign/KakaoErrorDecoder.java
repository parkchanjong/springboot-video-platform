package com.videoservice.manager.external.kakao.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoservice.manager.exception.ExternalException;
import com.videoservice.manager.external.kakao.KakaoErrorResponse;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KakaoErrorDecoder implements ErrorDecoder {
    private final ObjectMapper objectMapper;

    public KakaoErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            String body = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            KakaoErrorResponse errorResponse = objectMapper.readValue(body, KakaoErrorResponse.class);
            throw new ExternalException(errorResponse.message());
        } catch (IOException e) {
            log.error("[Kakao] 에러 메세지 파싱 에러 code={}, request={}, methodKey={}, errorMessage={}", response.status(), response.request(), methodKey, e.getMessage());
            throw new ExternalException("External API call error");
        }
    }
}
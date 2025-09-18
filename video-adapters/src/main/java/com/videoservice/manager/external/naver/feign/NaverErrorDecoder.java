package com.videoservice.manager.external.naver.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoservice.manager.exception.ExternalException;
import com.videoservice.manager.external.naver.NaverErrorResponse;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NaverErrorDecoder implements ErrorDecoder {
    private final ObjectMapper objectMapper;

    public NaverErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            String body = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            NaverErrorResponse errorResponse = objectMapper.readValue(body, NaverErrorResponse.class);
            throw new ExternalException(errorResponse.getErrorMessage());
        } catch (IOException e) {
            log.error("[Naver] 에러 메세지 파싱 에러 code={}, request={}, methodKey={}, errorMessage={}", response.status(), response.request(), methodKey, e.getMessage());
            throw new ExternalException("External API call error");
        }
    }
}
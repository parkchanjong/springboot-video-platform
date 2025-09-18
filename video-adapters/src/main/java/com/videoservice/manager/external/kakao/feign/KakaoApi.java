package com.videoservice.manager.external.kakao.feign;

import com.videoservice.manager.external.kakao.KakaoBookResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "kakaoClient", url = "${external.kakao.url}", configuration = KakaoClientConfiguration.class)
public interface KakaoApi {
    @GetMapping("/v3/search/book")
    KakaoBookResponse search(@RequestParam("query") String query,
            @RequestParam("page") int page,
            @RequestParam("size") int size);
}
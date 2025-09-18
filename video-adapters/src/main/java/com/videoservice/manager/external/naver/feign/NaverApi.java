package com.videoservice.manager.external.naver.feign;

import com.videoservice.manager.external.naver.NaverBookResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "naverClient", url = "${external.naver.url}", configuration = NaverClientConfiguration.class)
public interface NaverApi {
    @GetMapping("/v1/search/book.json")
    NaverBookResponse search(@RequestParam("query") String query,
            @RequestParam("start") int start,
            @RequestParam("display") int display);
}
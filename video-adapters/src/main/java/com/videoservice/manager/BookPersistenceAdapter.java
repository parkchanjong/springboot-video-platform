package com.videoservice.manager;

import com.videoservice.manager.book.Book;
import com.videoservice.manager.external.kakao.feign.KakaoApi;
import com.videoservice.manager.external.naver.feign.NaverApi;
import java.util.List;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookPersistenceAdapter implements LoadBookPort {

    private final NaverApi naverApi;
    private final KakaoApi kakaoApi;

    public BookPersistenceAdapter(NaverApi naverApi, KakaoApi kakaoApi) {
        this.naverApi = naverApi;
        this.kakaoApi = kakaoApi;
    }

    @Override
    @CircuitBreaker(name = "naverSearch", fallbackMethod = "searchFallBack")
    public List<Book> loadBook(String query, int page, int size) {
        var response = naverApi.search(query, page, size);
        return response.getItems().stream()
                .map(item -> Book.builder()
                                .title(item.getTitle())
                                .author(item.getAuthor())
                                .publisher(item.getPublisher())
                                .isbn(item.getIsbn())
                                .build()
                ).toList();
    }

    public List<Book> searchFallBack(String query, int page, int size, Throwable throwable) {
        if (throwable instanceof CallNotPermittedException) {
            return handleOpenCircuit(query, page, size);
        }
        return handleException(query, page, size, throwable);
    }

    private List<Book> handleOpenCircuit(String query, int page, int size) {
        log.warn("[BookQueryService] Circuit Breaker is open! Fallback to kakao search.");
        var response = kakaoApi.search(query, page, size);
        return response.documents().stream()
                .map(document -> Book.builder()
                                .title(document.title())
                                .author(document.authors().get(0))
                                .publisher(document.publisher())
                                .isbn(document.isbn())
                                .build()
                )
                .toList();
    }

    private List<Book> handleException(String query, int page, int size, Throwable throwable) {
        log.error("[BookQueryService] An error occurred! Fallback to kakao search. errorMessage={}", throwable.getMessage());
        var response = kakaoApi.search(query, page, size);
        return response.documents().stream()
                .map(document -> Book.builder()
                        .title(document.title())
                        .author(document.authors().get(0))
                        .publisher(document.publisher())
                        .isbn(document.isbn())
                        .build()
                )
                .toList();
    }
}

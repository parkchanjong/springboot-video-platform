package com.videoservice.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.videoservice.manager.external.kakao.Document;
import com.videoservice.manager.external.kakao.KakaoBookResponse;
import com.videoservice.manager.external.kakao.Meta;
import com.videoservice.manager.external.kakao.feign.KakaoApi;
import com.videoservice.manager.external.naver.Item;
import com.videoservice.manager.external.naver.NaverBookResponse;
import com.videoservice.manager.external.naver.feign.NaverApi;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookPersistenceAdapterTest {

    private BookPersistenceAdapter sut;

    private final NaverApi naverApi = mock(NaverApi.class);
    private final KakaoApi kakaoApi = mock(KakaoApi.class);

    @BeforeEach
    void setUp() {
        sut = new BookPersistenceAdapter(naverApi, kakaoApi);
    }

    @DisplayName("Delegates to NaverApi and maps items into Book domain objects")
    @Test
    void givenNaverApiResponds_whenLoadBook_thenMapsToDomainBooks() {
        // given
        var item = mock(Item.class);
        when(item.getTitle()).thenReturn("Clean Code");
        when(item.getAuthor()).thenReturn("Robert C. Martin");
        when(item.getPublisher()).thenReturn("Prentice Hall");
        when(item.getIsbn()).thenReturn("9780132350884");

        var response = mock(NaverBookResponse.class);
        when(response.getItems()).thenReturn(List.of(item));

        when(naverApi.search("clean code", 1, 10)).thenReturn(response);

        // when
        var books = sut.loadBook("clean code", 1, 10);

        // then
        assertThat(books).hasSize(1);
        var book = books.get(0);
        assertThat(book.getTitle()).isEqualTo("Clean Code");
        assertThat(book.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(book.getPublisher()).isEqualTo("Prentice Hall");
        assertThat(book.getIsbn()).isEqualTo("9780132350884");
        verify(naverApi).search("clean code", 1, 10);
        verifyNoInteractions(kakaoApi);
    }

    @DisplayName("Falls back to Kakao search when the circuit breaker is open")
    @Test
    void givenCircuitBreakerOpen_whenSearchFallBack_thenReturnsBooksFromKakao() {
        // given
        var document = new Document("Effective Java", List.of("Joshua Bloch"), "9780321356680", "Addison-Wesley", "2024-01-01");
        var kakaoResponse = new KakaoBookResponse(List.of(document), new Meta(false, 1, 1));
        when(kakaoApi.search("effective java", 2, 5)).thenReturn(kakaoResponse);

        var throwable = CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("naverSearch"));

        // when
        var books = sut.searchFallBack("effective java", 2, 5, throwable);

        // then
        assertThat(books).hasSize(1);
        var book = books.get(0);
        assertThat(book.getTitle()).isEqualTo("Effective Java");
        assertThat(book.getAuthor()).isEqualTo("Joshua Bloch");
        assertThat(book.getPublisher()).isEqualTo("Addison-Wesley");
        assertThat(book.getIsbn()).isEqualTo("9780321356680");
        verify(kakaoApi).search("effective java", 2, 5);
    }

    @DisplayName("Falls back to Kakao search when another error occurs")
    @Test
    void givenUnexpectedError_whenSearchFallBack_thenReturnsBooksFromKakao() {
        // given
        var document = new Document("Test Driven Development", List.of("Kent Beck"), "9780321146534", "Addison-Wesley", "2024-01-01");
        var kakaoResponse = new KakaoBookResponse(List.of(document), new Meta(true, 1, 1));
        when(kakaoApi.search("tdd", 3, 7)).thenReturn(kakaoResponse);

        // when
        var books = sut.searchFallBack("tdd", 3, 7, new RuntimeException("naver failure"));

        // then
        assertThat(books).hasSize(1);
        var book = books.get(0);
        assertThat(book.getTitle()).isEqualTo("Test Driven Development");
        assertThat(book.getAuthor()).isEqualTo("Kent Beck");
        assertThat(book.getPublisher()).isEqualTo("Addison-Wesley");
        assertThat(book.getIsbn()).isEqualTo("9780321146534");
        verify(kakaoApi).search("tdd", 3, 7);
    }
}

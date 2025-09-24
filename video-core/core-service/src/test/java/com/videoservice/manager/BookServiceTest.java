package com.videoservice.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.videoservice.manager.book.Book;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookServiceTest {

    private BookService sut;

    private final LoadBookPort loadBookPort = mock(LoadBookPort.class);

    @BeforeEach
    void setUp() {
        sut = new BookService(loadBookPort);
    }

    @Test
    @DisplayName("searchBook")
    void testSearchBook() {
        // Given
        var query = "Effective Java";
        var page = 0;
        var size = 5;
        var expectedBooks = List.of(
                Book.builder()
                        .title("Effective Java")
                        .author("Joshua Bloch")
                        .publisher("Addison-Wesley")
                        .pubDate(LocalDate.of(2018, 1, 6))
                        .isbn("978-0-1346-8726-7")
                        .build());
        when(loadBookPort.loadBook(query, page, size)).thenReturn(expectedBooks);

        // When
        var actual = sut.search(query, page, size);

        // Then
        assertThat(actual).isEqualTo(expectedBooks);
        verify(loadBookPort).loadBook(query, page, size);
    }
}

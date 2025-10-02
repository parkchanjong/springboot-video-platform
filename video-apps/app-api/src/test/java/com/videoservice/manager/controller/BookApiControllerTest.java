package com.videoservice.manager.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.videoservice.manager.BookUseCase;
import com.videoservice.manager.book.Book;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookApiController.class)
class BookApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookUseCase bookUseCase;

    @Test
    @DisplayName("GET /api/v1/book/list returns book search results")
    void testSearchBooks() throws Exception {
        // Given
        var query = "Effective Java";
        var page = 0;
        var size = 2;
        var books = List.of(
                Book.builder()
                        .title("Effective Java")
                        .author("Joshua Bloch")
                        .publisher("Addison-Wesley")
                        .pubDate(LocalDate.of(2018, 1, 6))
                        .isbn("978-0134685991")
                        .build(),
                Book.builder()
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .publisher("Prentice Hall")
                        .pubDate(LocalDate.of(2008, 8, 1))
                        .isbn("978-0132350884")
                        .build()
        );
        given(bookUseCase.search(query, page, size)).willReturn(books);

        // When & Then
        mockMvc
                .perform(
                        get("/api/v1/book/list")
                                .param("query", query)
                                .param("page", String.valueOf(page))
                                .param("size", String.valueOf(size))
                )
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.size()").value(2),
                        jsonPath("$[0].title").value("Effective Java"),
                        jsonPath("$[0].pubDate").value("2018-01-06")
                );

        // Then
        verify(bookUseCase).search(query, page, size);
    }
}

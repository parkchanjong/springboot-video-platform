package com.videoservice.manager.controller;

import static com.videoservice.manager.RestDocsUtils.requestPreprocessor;
import static com.videoservice.manager.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

import com.videoservice.manager.BookUseCase;
import com.videoservice.manager.RestDocsTest;
import com.videoservice.manager.domain.book.BookFixtures;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.payload.JsonFieldType;

class BookApiControllerTest extends RestDocsTest {

    private BookUseCase bookUseCase;

    private BookApiController controller;

    @BeforeEach
    void setUp() {
        bookUseCase = mock(BookUseCase.class);
        controller = new BookApiController(bookUseCase);
        mockMvc = mockController(controller);
    }

    @Test
    @DisplayName("GET /api/v1/book/list returns book search results")
    void searchBooks() {
        var query = "Effective Java";
        var page = 0;
        var size = 2;
        when(bookUseCase.search(eq(query), eq(page), eq(size))).thenReturn(
                List.of(BookFixtures.stub()));

                given().contentType(ContentType.JSON)
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .get("/api/v1/book/list")
                        .then()
                        .status(HttpStatus.OK)
                        .apply(document("books-search", requestPreprocessor(),
                                responsePreprocessor(),
                                queryParameters(
                                        parameterWithName("query").description("Search keyword"),
                                        parameterWithName("page").description(
                                                "Page number (zero based)"),
                                        parameterWithName("size").description("Page size")),
                                responseFields(
                                        fieldWithPath("[]").type(JsonFieldType.ARRAY)
                                                .description("List of books"),
                                        fieldWithPath("[].title").type(JsonFieldType.STRING)
                                                .description("Book title"),
                                        fieldWithPath("[].author").type(JsonFieldType.STRING)
                                                .description("Author name"),
                                        fieldWithPath("[].publisher").type(JsonFieldType.STRING)
                                                .description("Publisher name"),
                                        fieldWithPath("[].pubDate").type(JsonFieldType.STRING)
                                                .description("Publication date (ISO-8601)"),
                                        fieldWithPath("[].isbn").type(JsonFieldType.STRING)
                                                .description("ISBN code"))));
    }
}

package com.videoservice.manager.controller;

import com.videoservice.manager.BookUseCase;
import com.videoservice.manager.book.Book;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/book")
public class BookApiController {
    private final BookUseCase bookUseCase;

    public BookApiController(BookUseCase bookUseCase) {
        this.bookUseCase = bookUseCase;
    }

    @GetMapping(value = "list", params = {"query", "page", "size"})
    List<Book> search(
            @RequestParam String query,
            @RequestParam int page,
            @RequestParam int size
    ) {
        return bookUseCase.search(query, page, size);
    }
}

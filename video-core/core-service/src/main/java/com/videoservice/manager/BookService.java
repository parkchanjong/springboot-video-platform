package com.videoservice.manager;

import com.videoservice.manager.book.Book;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookService implements BookUseCase {

    private final LoadBookPort loadBookPort;

    public BookService(LoadBookPort loadBookPort) {
        this.loadBookPort = loadBookPort;
    }

    @Override
    public List<Book> search(String query, int page, int size) {
        return loadBookPort.loadBook(query, page, size);
    }
}

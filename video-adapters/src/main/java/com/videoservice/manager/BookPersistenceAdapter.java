package com.videoservice.manager;

import com.videoservice.manager.book.Book;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookPersistenceAdapter implements LoadBookPort {

    @Override
    public List<Book> loadBook(String query, int page, int size) {
        return Collections.emptyList();
    }
}

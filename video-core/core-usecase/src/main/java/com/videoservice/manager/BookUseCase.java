package com.videoservice.manager;

import com.videoservice.manager.book.Book;
import java.util.List;

public interface BookUseCase {
    List<Book> search(String query, int page, int size);
}

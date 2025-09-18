package com.videoservice.manager;

import com.videoservice.manager.book.Book;
import java.util.List;

public interface LoadBookPort {
    List<Book> loadBook(String query, int page, int size);
}

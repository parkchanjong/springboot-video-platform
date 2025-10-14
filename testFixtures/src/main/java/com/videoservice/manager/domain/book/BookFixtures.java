package com.videoservice.manager.domain.book;

import com.videoservice.manager.book.Book;
import java.time.LocalDate;

public class BookFixtures {
    public static Book stub() {
        return Book.builder()
                .title("Effective Java")
                .author("Joshua Bloch")
                .publisher("Addison-Wesley")
                .pubDate(LocalDate.of(2008, 12, 30))
                .isbn("978-0321356680")
                .build();
    }
}

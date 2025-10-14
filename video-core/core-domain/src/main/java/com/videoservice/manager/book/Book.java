package com.videoservice.manager.book;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class Book {
    String title;
    String author;
    String publisher;
    LocalDate pubDate;
    String isbn;
}

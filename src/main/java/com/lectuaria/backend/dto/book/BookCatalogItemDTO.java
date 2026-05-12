package com.lectuaria.backend.dto.book;

import java.time.Instant;

public class BookCatalogItemDTO {
    private BookSummaryDTO book;
    private Instant addedAt;

    public BookCatalogItemDTO() {}

    public BookCatalogItemDTO(BookSummaryDTO book, Instant addedAt) {
        this.book = book;
        this.addedAt = addedAt;
    }

    public BookSummaryDTO getBook() { return book; }
    public void setBook(BookSummaryDTO book) { this.book = book; }

    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
}

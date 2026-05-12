package com.lectuaria.backend.dto.book;

public class BookPublishResponseDTO {
    private final Long bookId;
    private final String title;
    private final Long isbn;
    private final boolean isNewBook; // true si se creó, false si ya existía
    private final String message;

    public BookPublishResponseDTO(Long bookId, String title, Long isbn, boolean isNewBook, String message) {
        this.bookId = bookId;
        this.title = title;
        this.isbn = isbn;
        this.isNewBook = isNewBook;
        this.message = message;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public Long getIsbn() {
        return isbn;
    }

    public boolean isNewBook() {
        return isNewBook;
    }

    public String getMessage() {
        return message;
    }
}

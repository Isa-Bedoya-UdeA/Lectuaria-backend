package com.lectuaria.backend.dto.book;

import java.math.BigDecimal;

public class BookReviewPreviewResponseDTO {
    private final Long bookId;
    private final BigDecimal rating;
    private final String reviewText;
    private final int remainingCharacters;

    public BookReviewPreviewResponseDTO(Long bookId, BigDecimal rating, String reviewText, int remainingCharacters) {
        this.bookId = bookId;
        this.rating = rating;
        this.reviewText = reviewText;
        this.remainingCharacters = remainingCharacters;
    }

    public Long getBookId() {
        return bookId;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public int getRemainingCharacters() {
        return remainingCharacters;
    }
}

package com.lectuaria.backend.dto.book;

import java.math.BigDecimal;

public class BookRatingResponseDTO {

    private final String message;
    private final Long bookId;
    private final BigDecimal userRating;
    private final BigDecimal averageRating;
    private final Long ratingsCount;
    private final Long reviewId;
    private final String reviewText;
    private final String reviewStatus;

    public BookRatingResponseDTO(String message, Long bookId, BigDecimal userRating, BigDecimal averageRating,
            Long ratingsCount, Long reviewId, String reviewText, String reviewStatus) {
        this.message = message;
        this.bookId = bookId;
        this.userRating = userRating;
        this.averageRating = averageRating;
        this.ratingsCount = ratingsCount;
        this.reviewId = reviewId;
        this.reviewText = reviewText;
        this.reviewStatus = reviewStatus;
    }

    public String getMessage() {
        return message;
    }

    public Long getBookId() {
        return bookId;
    }

    public BigDecimal getUserRating() {
        return userRating;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public Long getRatingsCount() {
        return ratingsCount;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public String getReviewText() {
        return reviewText;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }
}

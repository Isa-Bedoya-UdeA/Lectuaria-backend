package com.lectuaria.backend.dto.book;

import java.math.BigDecimal;
import java.time.Instant;

public class BookReviewResponseDTO {

    private final Long reviewId;
    private final Long bookId;
    private final Long authorId;
    private final String authorName;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final BigDecimal rating;
    private final String reviewText;
    private final String status;
    private final Integer helpfulCount;
    private final boolean friendAuthor;

    public BookReviewResponseDTO(Long reviewId, Long bookId, Long authorId, String authorName,
            Instant createdAt, Instant updatedAt, BigDecimal rating, String reviewText,
            String status, Integer helpfulCount, boolean friendAuthor) {
        this.reviewId = reviewId;
        this.bookId = bookId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.rating = rating;
        this.reviewText = reviewText;
        this.status = status;
        this.helpfulCount = helpfulCount;
        this.friendAuthor = friendAuthor;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public Long getBookId() {
        return bookId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public String getStatus() {
        return status;
    }

    public Integer getHelpfulCount() {
        return helpfulCount;
    }

    public boolean isFriendAuthor() {
        return friendAuthor;
    }
}

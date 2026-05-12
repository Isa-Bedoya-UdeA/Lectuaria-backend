package com.lectuaria.backend.dto.book;

import java.math.BigDecimal;
import java.time.Instant;

public class BookRatingWithUserDTO {

    private final Long id;
    private final Long bookId;
    private final BigDecimal rating;
    private final Long userId;
    private final String userName;
    private final String userEmail;
    private final Instant createdAt;

    public BookRatingWithUserDTO(Long id, Long bookId, BigDecimal rating, Long userId, String userName, String userEmail, Instant createdAt) {
        this.id = id;
        this.bookId = bookId;
        this.rating = rating;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getBookId() {
        return bookId;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.lectuaria.backend.dto.recommendation;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import java.time.Instant;

public class RecommendationDTO {
    private BookSummaryDTO book;
    private String reason;
    private Instant generatedAt;
    private Instant nextRefreshAt;

    public RecommendationDTO() {}

    public RecommendationDTO(BookSummaryDTO book, String reason, Instant generatedAt, Instant nextRefreshAt) {
        this.book = book;
        this.reason = reason;
        this.generatedAt = generatedAt;
        this.nextRefreshAt = nextRefreshAt;
    }

    public BookSummaryDTO getBook() { return book; }
    public void setBook(BookSummaryDTO book) { this.book = book; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    public Instant getNextRefreshAt() { return nextRefreshAt; }
    public void setNextRefreshAt(Instant nextRefreshAt) { this.nextRefreshAt = nextRefreshAt; }
}

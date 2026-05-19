package com.lectuaria.backend.dto.statistics;

import com.lectuaria.backend.dto.book.BookSummaryDTO;

public class PopularLibraryBookDTO {
    private BookSummaryDTO book;
    private Long interactions;
    private Long reviewsCount;
    private Integer ratingsCount;

    public PopularLibraryBookDTO() {}

    public PopularLibraryBookDTO(BookSummaryDTO book, Long interactions, Long reviewsCount, Integer ratingsCount) {
        this.book = book;
        this.interactions = interactions;
        this.reviewsCount = reviewsCount;
        this.ratingsCount = ratingsCount;
    }

    public BookSummaryDTO getBook() { return book; }
    public void setBook(BookSummaryDTO book) { this.book = book; }
    public Long getInteractions() { return interactions; }
    public void setInteractions(Long interactions) { this.interactions = interactions; }
    public Long getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(Long reviewsCount) { this.reviewsCount = reviewsCount; }
    public Integer getRatingsCount() { return ratingsCount; }
    public void setRatingsCount(Integer ratingsCount) { this.ratingsCount = ratingsCount; }
}

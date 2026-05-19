package com.lectuaria.backend.dto.statistics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class LibraryStatisticsDTO {
    private Long totalBooks;
    private Long booksAddedThisMonth;
    private List<GenreCountDTO> mostRepresentedGenres;
    private Long reviewsOnOwnBooks;
    private BigDecimal averageRatingOfOwnBooks;
    private List<PopularLibraryBookDTO> mostPopularBooks;
    private Instant updatedAt;
    private Instant nextRefreshAt;

    public LibraryStatisticsDTO() {}

    public LibraryStatisticsDTO(Long totalBooks, Long booksAddedThisMonth, List<GenreCountDTO> mostRepresentedGenres,
            Long reviewsOnOwnBooks, BigDecimal averageRatingOfOwnBooks, List<PopularLibraryBookDTO> mostPopularBooks,
            Instant updatedAt, Instant nextRefreshAt) {
        this.totalBooks = totalBooks;
        this.booksAddedThisMonth = booksAddedThisMonth;
        this.mostRepresentedGenres = mostRepresentedGenres;
        this.reviewsOnOwnBooks = reviewsOnOwnBooks;
        this.averageRatingOfOwnBooks = averageRatingOfOwnBooks;
        this.mostPopularBooks = mostPopularBooks;
        this.updatedAt = updatedAt;
        this.nextRefreshAt = nextRefreshAt;
    }

    public Long getTotalBooks() { return totalBooks; }
    public Long getBooksAddedThisMonth() { return booksAddedThisMonth; }
    public List<GenreCountDTO> getMostRepresentedGenres() { return mostRepresentedGenres; }
    public Long getReviewsOnOwnBooks() { return reviewsOnOwnBooks; }
    public BigDecimal getAverageRatingOfOwnBooks() { return averageRatingOfOwnBooks; }
    public List<PopularLibraryBookDTO> getMostPopularBooks() { return mostPopularBooks; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getNextRefreshAt() { return nextRefreshAt; }
}

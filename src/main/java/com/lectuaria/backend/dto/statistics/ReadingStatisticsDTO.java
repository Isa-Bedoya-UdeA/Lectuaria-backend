package com.lectuaria.backend.dto.statistics;

import java.time.Instant;
import java.util.List;

public class ReadingStatisticsDTO {
    private Long totalBooksRead;
    private Integer reviewsCount;
    private List<GenreCountDTO> mostReadGenres;
    private List<MonthlyBooksReadDTO> booksReadByMonth;
    private YearComparisonDTO yearComparison;
    private Instant updatedAt;

    public ReadingStatisticsDTO() {}

    public ReadingStatisticsDTO(Long totalBooksRead, Integer reviewsCount, List<GenreCountDTO> mostReadGenres,
            List<MonthlyBooksReadDTO> booksReadByMonth, YearComparisonDTO yearComparison, Instant updatedAt) {
        this.totalBooksRead = totalBooksRead;
        this.reviewsCount = reviewsCount;
        this.mostReadGenres = mostReadGenres;
        this.booksReadByMonth = booksReadByMonth;
        this.yearComparison = yearComparison;
        this.updatedAt = updatedAt;
    }

    public Long getTotalBooksRead() { return totalBooksRead; }
    public void setTotalBooksRead(Long totalBooksRead) { this.totalBooksRead = totalBooksRead; }
    public Integer getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(Integer reviewsCount) { this.reviewsCount = reviewsCount; }
    public List<GenreCountDTO> getMostReadGenres() { return mostReadGenres; }
    public void setMostReadGenres(List<GenreCountDTO> mostReadGenres) { this.mostReadGenres = mostReadGenres; }
    public List<MonthlyBooksReadDTO> getBooksReadByMonth() { return booksReadByMonth; }
    public void setBooksReadByMonth(List<MonthlyBooksReadDTO> booksReadByMonth) { this.booksReadByMonth = booksReadByMonth; }
    public YearComparisonDTO getYearComparison() { return yearComparison; }
    public void setYearComparison(YearComparisonDTO yearComparison) { this.yearComparison = yearComparison; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

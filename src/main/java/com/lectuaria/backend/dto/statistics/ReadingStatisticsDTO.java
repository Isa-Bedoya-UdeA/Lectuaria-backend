package com.lectuaria.backend.dto.statistics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ReadingStatisticsDTO {
    private Long totalBooksRead;
    private BigDecimal averageRatingGiven;
    private List<GenreCountDTO> mostReadGenres;
    private List<MonthlyBooksReadDTO> booksReadByMonth;
    private YearComparisonDTO yearComparison;
    private Instant updatedAt;

    public ReadingStatisticsDTO() {}

    public ReadingStatisticsDTO(Long totalBooksRead, BigDecimal averageRatingGiven, List<GenreCountDTO> mostReadGenres,
            List<MonthlyBooksReadDTO> booksReadByMonth, YearComparisonDTO yearComparison, Instant updatedAt) {
        this.totalBooksRead = totalBooksRead;
        this.averageRatingGiven = averageRatingGiven;
        this.mostReadGenres = mostReadGenres;
        this.booksReadByMonth = booksReadByMonth;
        this.yearComparison = yearComparison;
        this.updatedAt = updatedAt;
    }

    public Long getTotalBooksRead() { return totalBooksRead; }
    public void setTotalBooksRead(Long totalBooksRead) { this.totalBooksRead = totalBooksRead; }
    public BigDecimal getAverageRatingGiven() { return averageRatingGiven; }
    public void setAverageRatingGiven(BigDecimal averageRatingGiven) { this.averageRatingGiven = averageRatingGiven; }
    public List<GenreCountDTO> getMostReadGenres() { return mostReadGenres; }
    public void setMostReadGenres(List<GenreCountDTO> mostReadGenres) { this.mostReadGenres = mostReadGenres; }
    public List<MonthlyBooksReadDTO> getBooksReadByMonth() { return booksReadByMonth; }
    public void setBooksReadByMonth(List<MonthlyBooksReadDTO> booksReadByMonth) { this.booksReadByMonth = booksReadByMonth; }
    public YearComparisonDTO getYearComparison() { return yearComparison; }
    public void setYearComparison(YearComparisonDTO yearComparison) { this.yearComparison = yearComparison; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

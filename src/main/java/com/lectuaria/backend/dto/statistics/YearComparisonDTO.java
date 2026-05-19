package com.lectuaria.backend.dto.statistics;

public class YearComparisonDTO {
    private Integer currentYear;
    private Long currentYearBooks;
    private Integer previousYear;
    private Long previousYearBooks;
    private Long difference;

    public YearComparisonDTO() {}

    public YearComparisonDTO(Integer currentYear, Long currentYearBooks, Integer previousYear, Long previousYearBooks) {
        this.currentYear = currentYear;
        this.currentYearBooks = currentYearBooks;
        this.previousYear = previousYear;
        this.previousYearBooks = previousYearBooks;
        this.difference = currentYearBooks - previousYearBooks;
    }

    public Integer getCurrentYear() { return currentYear; }
    public void setCurrentYear(Integer currentYear) { this.currentYear = currentYear; }
    public Long getCurrentYearBooks() { return currentYearBooks; }
    public void setCurrentYearBooks(Long currentYearBooks) { this.currentYearBooks = currentYearBooks; }
    public Integer getPreviousYear() { return previousYear; }
    public void setPreviousYear(Integer previousYear) { this.previousYear = previousYear; }
    public Long getPreviousYearBooks() { return previousYearBooks; }
    public void setPreviousYearBooks(Long previousYearBooks) { this.previousYearBooks = previousYearBooks; }
    public Long getDifference() { return difference; }
    public void setDifference(Long difference) { this.difference = difference; }
}

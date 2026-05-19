package com.lectuaria.backend.dto.statistics;

public class MonthlyBooksReadDTO {
    private Integer month;
    private Long booksRead;

    public MonthlyBooksReadDTO() {}

    public MonthlyBooksReadDTO(Integer month, Long booksRead) {
        this.month = month;
        this.booksRead = booksRead;
    }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public Long getBooksRead() { return booksRead; }
    public void setBooksRead(Long booksRead) { this.booksRead = booksRead; }
}

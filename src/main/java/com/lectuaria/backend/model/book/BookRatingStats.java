package com.lectuaria.backend.model.book;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "BOOK_RATING_STATS")
public class BookRatingStats {

    @Id
    @Column(name = "id_book")
    private Long bookId;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "total_ratings")
    private Integer totalRatings;

    // Constructor por defecto
    public BookRatingStats() {}

    // Constructor con parámetros
    public BookRatingStats(Long bookId, BigDecimal avgRating, Integer totalRatings) {
        this.bookId = bookId;
        this.avgRating = avgRating;
        this.totalRatings = totalRatings;
    }

    // Getters y Setters
    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }

    // Método de utilidad para actualizar estadísticas
    public void updateStats(BigDecimal newAvgRating, Integer newTotalRatings) {
        this.avgRating = newAvgRating != null ? newAvgRating : BigDecimal.ZERO;
        this.totalRatings = newTotalRatings != null ? newTotalRatings : 0;
    }
}

package com.lectuaria.backend.dto.book;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class BookReviewPreviewRequestDTO {

    @NotNull(message = "La calificación es obligatoria")
    @DecimalMin(value = "1.0", message = "La calificación mínima es 1.0")
    @DecimalMax(value = "5.0", message = "La calificación máxima es 5.0")
    private BigDecimal rating;

    @NotBlank(message = "La reseña no puede estar vacía")
    @Size(max = 2000, message = "La reseña no puede superar 2000 caracteres")
    private String reviewText;

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }
}

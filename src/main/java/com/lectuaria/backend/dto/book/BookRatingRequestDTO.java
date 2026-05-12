package com.lectuaria.backend.dto.book;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;

public class BookRatingRequestDTO {

    @NotNull(message = "La calificación es obligatoria")
    @DecimalMin(value = "1.0", message = "La calificación mínima es 1.0")
    @DecimalMax(value = "5.0", message = "La calificación máxima es 5.0")
    private BigDecimal rating;

    public @NonNull BigDecimal getRating() {
        return java.util.Objects.requireNonNull(rating);
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    @AssertTrue(message = "La calificación solo permite incrementos de 0.5")
    public boolean isHalfStarStep() {
        if (rating == null) {
            return true;
        }
        return rating.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() <= 0;
    }
}

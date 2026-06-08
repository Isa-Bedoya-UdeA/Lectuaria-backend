package com.lectuaria.backend.service.book.search;

import com.lectuaria.backend.dto.book.BookFilterDTO;
import com.lectuaria.backend.specification.BookSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Strategy (GoF) que filtra libros con rating promedio >= al indicado.
 * Solo aplica si {@code minRating} es no nulo y mayor que 0.
 */
@Component
public class MinRatingFilterStrategy implements BookFilterStrategy {

    @Override
    public String name() {
        return "min-rating";
    }

    @Override
    public boolean applies(BookFilterDTO filter) {
        return filter.getMinRating() != null && filter.getMinRating() > 0;
    }

    @Override
    public Specification<?> toSpec(BookFilterDTO filter) {
        return BookSpecifications.hasMinimumRating(filter.getMinRating());
    }
}

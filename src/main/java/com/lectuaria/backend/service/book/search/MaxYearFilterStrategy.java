package com.lectuaria.backend.service.book.search;

import com.lectuaria.backend.dto.book.BookFilterDTO;
import com.lectuaria.backend.specification.BookSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Strategy (GoF) que filtra libros con anio de publicacion <= al indicado.
 * Solo aplica si {@code maxYear} es no nulo.
 */
@Component
public class MaxYearFilterStrategy implements BookFilterStrategy {

    @Override
    public String name() {
        return "max-year";
    }

    @Override
    public boolean applies(BookFilterDTO filter) {
        return filter.getMaxYear() != null;
    }

    @Override
    public Specification<?> toSpec(BookFilterDTO filter) {
        return BookSpecifications.hasMaxPublicationYear(filter.getMaxYear());
    }
}

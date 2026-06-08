package com.lectuaria.backend.service.book.search;

import com.lectuaria.backend.dto.book.BookFilterDTO;
import com.lectuaria.backend.specification.BookSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Strategy (GoF) que filtra libros con anio de publicacion >= al indicado.
 * Solo aplica si {@code minYear} es no nulo.
 */
@Component
public class MinYearFilterStrategy implements BookFilterStrategy {

    @Override
    public String name() {
        return "min-year";
    }

    @Override
    public boolean applies(BookFilterDTO filter) {
        return filter.getMinYear() != null;
    }

    @Override
    public Specification<?> toSpec(BookFilterDTO filter) {
        return BookSpecifications.hasMinPublicationYear(filter.getMinYear());
    }
}

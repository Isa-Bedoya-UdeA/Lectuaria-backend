package com.lectuaria.backend.service.book.search;

import com.lectuaria.backend.dto.book.BookFilterDTO;
import com.lectuaria.backend.specification.BookSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy (GoF) que filtra libros por los formatos disponibles
 * (physical, digital). Solo aplica si {@code formatTypes} trae al
 * menos un valor.
 */
@Component
public class FormatFilterStrategy implements BookFilterStrategy {

    @Override
    public String name() {
        return "format";
    }

    @Override
    public boolean applies(BookFilterDTO filter) {
        return filter.getFormatTypes() != null && !filter.getFormatTypes().isEmpty();
    }

    @Override
    public Specification<?> toSpec(BookFilterDTO filter) {
        List<String> formatTypes = filter.getFormatTypes();
        return BookSpecifications.hasFormatTypes(formatTypes);
    }
}

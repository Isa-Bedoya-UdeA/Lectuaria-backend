package com.lectuaria.backend.service.book.search;

import com.lectuaria.backend.dto.book.BookFilterDTO;
import org.springframework.data.jpa.domain.Specification;

/**
 * Strategy (GoF) que encapsula UNA condicion de busqueda de libros.
 *
 * Cada filtro (keywords, generos, bibliotecas, formato, anos, rating) decide
 * si aplica a un {@link BookFilterDTO} dado y, si aplica, devuelve la
 * {@link Specification} correspondiente. El {@code BookServiceImpl} compone
 * todas las strategies relevantes encadenandolas con {@code Specification.and}.
 */
public interface BookFilterStrategy {

    /**
     * @return nombre legible del filtro (util para logs y debugging).
     */
    String name();

    /**
     * Indica si este filtro aporta condicion para esta busqueda.
     * Por ejemplo, un filtro de rating devuelve {@code true} solo si
     * el DTO trae un minRating distinto de null y mayor que 0.
     */
    boolean applies(BookFilterDTO filter);

    /**
     * Construye la {@link Specification} que representa este filtro.
     * Solo se llama si {@link #applies(BookFilterDTO)} devolvio true.
     */
    Specification<?> toSpec(BookFilterDTO filter);
}

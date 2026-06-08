package com.lectuaria.backend.util;

import com.lectuaria.backend.controller.books.BookController;
import com.lectuaria.backend.controller.books.BookRatingController;
import com.lectuaria.backend.dto.book.BookDetailDTO;
import org.springframework.hateoas.EntityModel;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Factory Method (GoF) para construir el {@link EntityModel} hipermedia
 * del recurso {@link BookDetailDTO}.
 *
 * Centraliza las relaciones hipermedia relevantes del libro (self, similar,
 * ratings, reviews, share-link) para que cualquier endpoint que retorne un
 * libro las exponga de forma consistente. Si en el futuro se agregan mas
 * relaciones, se modifican aqui y todos los endpoints las reciben.
 *
 * Nota: aplica el patron Factory Method de GoF (un metodo creador que
 * devuelve instancias del tipo abstracto EntityModel) sobre el recurso
 * concreto BookDetailDTO.
 */
public final class BookResponseFactory {

    private BookResponseFactory() {}

    /**
     * Construye un EntityModel con todas las relaciones hipermedia del libro.
     */
    public static EntityModel<BookDetailDTO> wrap(BookDetailDTO book) {
        if (book == null) {
            return null;
        }
        Long id = book.getId();
        return EntityModel.of(book,
                linkTo(methodOn(BookController.class).getBookById(id)).withSelfRel(),
                linkTo(methodOn(BookController.class).getSimilarBooks(id)).withRel("similar"),
                linkTo(methodOn(BookController.class).getBookShareLink(id)).withRel("share-link"),
                linkTo(methodOn(BookRatingController.class).getAllBookRatings(id)).withRel("ratings"),
                linkTo(methodOn(BookController.class).getBookByIsbn(book.getIsbn())).withRel("by-isbn"));
    }
}

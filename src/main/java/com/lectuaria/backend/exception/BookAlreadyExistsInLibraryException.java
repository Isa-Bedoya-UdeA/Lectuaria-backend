package com.lectuaria.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Intento de agregar un libro que ya esta en el inventario. HTTP 409.
 */
public class BookAlreadyExistsInLibraryException extends DomainException {
    public BookAlreadyExistsInLibraryException(String message) {
        super(message, HttpStatus.CONFLICT, "BOOK_ALREADY_IN_LIBRARY");
    }
}

package com.lectuaria.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Error al consumir la API externa de Google Books. HTTP 502.
 */
public class GoogleBooksApiException extends DomainException {

    public GoogleBooksApiException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "GOOGLE_BOOKS_API_ERROR");
    }

    public GoogleBooksApiException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, "GOOGLE_BOOKS_API_ERROR", cause);
    }
}

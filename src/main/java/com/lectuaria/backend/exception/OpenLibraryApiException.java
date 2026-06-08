package com.lectuaria.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Error al consumir la API externa de OpenLibrary. HTTP 502.
 */
public class OpenLibraryApiException extends DomainException {
    public OpenLibraryApiException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "OPEN_LIBRARY_API_ERROR");
    }

    public OpenLibraryApiException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, "OPEN_LIBRARY_API_ERROR", cause);
    }
}

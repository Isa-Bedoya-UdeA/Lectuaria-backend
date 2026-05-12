package com.lectuaria.backend.exception;

public class OpenLibraryApiException extends RuntimeException {
    public OpenLibraryApiException(String message) {
        super(message);
    }

    public OpenLibraryApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
package com.lectuaria.backend.exception;

public class BookAlreadyExistsInLibraryException extends RuntimeException {
    public BookAlreadyExistsInLibraryException(String message) {
        super(message);
    }
}
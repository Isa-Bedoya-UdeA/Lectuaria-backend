package com.lectuaria.backend.exception.list;

public class AlreadySharedException extends RuntimeException {
    public AlreadySharedException(String message) {
        super(message);
    }
}

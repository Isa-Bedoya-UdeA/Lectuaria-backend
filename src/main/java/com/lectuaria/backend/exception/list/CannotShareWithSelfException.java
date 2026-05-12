package com.lectuaria.backend.exception.list;

public class CannotShareWithSelfException extends RuntimeException {
    public CannotShareWithSelfException(String message) {
        super(message);
    }
}

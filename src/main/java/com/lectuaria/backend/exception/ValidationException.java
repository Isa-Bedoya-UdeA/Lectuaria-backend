package com.lectuaria.backend.exception;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("No se pudo completar el registro.");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}

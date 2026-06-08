package com.lectuaria.backend.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Excepción que representa un conflicto de estado (por ejemplo, intentar
 * crear un registro duplicado). Se traduce a HTTP 409 Conflict.
 */
public class ConflictException extends BusinessException {

    private final List<String> errors;

    public ConflictException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String getCode() {
        return "CONFLICT";
    }
}

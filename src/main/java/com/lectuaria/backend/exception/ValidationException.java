package com.lectuaria.backend.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Excepción para errores de validación semántica (más allá de las anotaciones
 * Bean Validation de los DTOs). Devuelve una lista de mensajes de error.
 * Se traduce a HTTP 400 Bad Request.
 */
public class ValidationException extends DomainException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("No se pudo completar la solicitud.", HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        this.errors = errors;
    }

    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        this.errors = List.of(message);
    }

    public List<String> getErrors() {
        return errors;
    }
}

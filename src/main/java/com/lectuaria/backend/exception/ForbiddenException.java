package com.lectuaria.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando el usuario está autenticado pero no tiene
 * permisos suficientes. Se traduce a HTTP 403 Forbidden.
 */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
}

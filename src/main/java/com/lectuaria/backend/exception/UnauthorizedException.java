package com.lectuaria.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando el usuario no está autenticado o el token
 * no es válido. Se traduce a HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}

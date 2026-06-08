package com.lectuaria.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción que representa una violación de una regla de negocio.
 * Se traduce a HTTP 400 Bad Request.
 */
public class BusinessException extends DomainException {

    public BusinessException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION");
    }
}

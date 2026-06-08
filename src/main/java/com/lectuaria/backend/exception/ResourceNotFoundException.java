package com.lectuaria.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando un recurso solicitado no existe.
 * Se traduce a HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}

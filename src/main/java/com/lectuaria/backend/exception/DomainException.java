package com.lectuaria.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Clase base de la jerarquía de excepciones de dominio de Lectuaria.
 * Todas las excepciones de negocio deben heredar de esta clase, lo que
 * permite un manejo centralizado en el {@link GlobalExceptionHandler}.
 *
 * Cada subclase concreta define un {@link HttpStatus} representativo del
 * tipo de error (negocio, recurso no encontrado, autorización, etc.).
 */
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected DomainException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    protected DomainException(String message, HttpStatus status, String code, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}

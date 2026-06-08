package com.lectuaria.backend.exception.list;

import com.lectuaria.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Token de comparticion invalido o expirado al intentar acceder a una
 * lista compartida. HTTP 401.
 *
 * Nota: se mantiene separada de la excepcion raiz {@code TokenException}
 * (que es para tokens JWT) para preservar la semantica.
 */
public class InvalidShareTokenException extends DomainException {
    public InvalidShareTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "INVALID_SHARE_TOKEN");
    }
}

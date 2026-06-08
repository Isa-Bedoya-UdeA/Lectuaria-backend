package com.lectuaria.backend.exception;

/**
 * Excepción lanzada cuando las credenciales proporcionadas son inválidas.
 * Hereda de {@link UnauthorizedException} para mantener el contrato de status HTTP.
 */
public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    @Override
    public String getCode() {
        return "INVALID_CREDENTIALS";
    }
}

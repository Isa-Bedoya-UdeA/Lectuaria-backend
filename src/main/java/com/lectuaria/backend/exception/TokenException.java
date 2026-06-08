package com.lectuaria.backend.exception;

/**
 * Excepción lanzada cuando el token JWT es inválido, expiró o está mal formado.
 * Hereda de {@link UnauthorizedException} para mantener el contrato HTTP 401.
 */
public class TokenException extends UnauthorizedException {

    public TokenException(String message) {
        super(message);
    }

    @Override
    public String getCode() {
        return "INVALID_TOKEN";
    }
}

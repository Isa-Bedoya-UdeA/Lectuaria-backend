package com.lectuaria.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO estandar para todas las respuestas de error de la API.
 * Diseño: Decorator Pattern aplicado al manejo de errores: este DTO
 * siempre se devuelve con la misma estructura, enriquecida con
 * informacion de trazabilidad (traceId) y timestamp.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {

    private final String message;
    private final List<String> errors;
    private final String code;
    private final String traceId;
    private final Instant timestamp;

    public ErrorResponseDTO(String message, List<String> errors) {
        this(message, errors, "ERROR", UUID.randomUUID().toString());
    }

    public ErrorResponseDTO(String message, List<String> errors, String code, String traceId) {
        this.message = message;
        this.errors = errors;
        this.code = code;
        this.traceId = traceId;
        this.timestamp = Instant.now();
    }

    public String getMessage() {
        return message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getCode() {
        return code;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

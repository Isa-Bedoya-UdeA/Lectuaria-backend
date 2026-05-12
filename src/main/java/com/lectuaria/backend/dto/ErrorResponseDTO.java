package com.lectuaria.backend.dto;

import java.util.List;

public class ErrorResponseDTO {
    private final String message;
    private final List<String> errors;

    public ErrorResponseDTO(String message, List<String> errors) {
        this.message = message;
        this.errors = errors;
    }

    public String getMessage() { return message; }
    public List<String> getErrors() { return errors; }
}

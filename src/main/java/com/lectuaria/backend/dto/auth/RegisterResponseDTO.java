package com.lectuaria.backend.dto.auth;

public class RegisterResponseDTO {
    private final String message;
    private final String userRole;

    public RegisterResponseDTO(String message, String userRole) {
        this.message = message;
        this.userRole = userRole;
    }

    public String getMessage() {
        return message;
    }

    public String getUserRole() {
        return userRole;
    }
}
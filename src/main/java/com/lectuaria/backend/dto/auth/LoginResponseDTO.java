package com.lectuaria.backend.dto.auth;

public class LoginResponseDTO {

    private final String message;
    private final String accessToken;
    private final String refreshToken;

    public LoginResponseDTO(String message, String accessToken, String refreshToken) {
        this.message = message;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getMessage() {
        return message;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

}
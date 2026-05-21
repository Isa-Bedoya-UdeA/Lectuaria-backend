package com.lectuaria.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class PasswordResetRequestDTO {

    @NotBlank(message = "El token es requerido")
    private String token;

    @NotBlank(message = "La nueva contraseña es requerida")
    private String newPassword;

    @NotBlank(message = "La confirmación de contraseña es requerida")
    private String confirmPassword;

    public PasswordResetRequestDTO() {}

    public PasswordResetRequestDTO(String token, String newPassword, String confirmPassword) {
        this.token = token;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
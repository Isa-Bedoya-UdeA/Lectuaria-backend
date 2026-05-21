package com.lectuaria.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequestDTO {

    @NotBlank(message = "El correo es requerido")
    @Email(message = "Correo inválido")
    private String email;

    public ForgotPasswordRequestDTO() {}

    public ForgotPasswordRequestDTO(String email) {
        this.email = email;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
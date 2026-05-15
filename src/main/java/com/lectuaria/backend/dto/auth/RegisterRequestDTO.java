package com.lectuaria.backend.dto.auth;

import com.lectuaria.backend.dto.library.LibraryRequestDTO;
import com.lectuaria.backend.model.auth.UserRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RegisterRequestDTO {

    @NotBlank(message = "El nombre completo es obligatorio.")
    private String fullName;

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El correo electrónico no tiene un formato válido.")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria.")
    private String password;

    @NotBlank(message = "La confirmación de contraseña es obligatoria.")
    private String confirmPassword;

    @NotNull(message = "El rol de usuario es obligatorio.")
    private UserRole userRole;

    // Para usuarios lectores
    private String username;

    // Para bibliotecarios (objeto anidado)
    @Valid
    private LibraryRequestDTO library;

    // Getters y Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LibraryRequestDTO getLibrary() {
        return library;
    }

    public void setLibrary(LibraryRequestDTO library) {
        this.library = library;
    }
}
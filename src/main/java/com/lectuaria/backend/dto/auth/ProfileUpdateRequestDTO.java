package com.lectuaria.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class ProfileUpdateRequestDTO {
    // ===== Campos para usuarios LECTORES =====
    @Size(max = 50, message = "El nombre de usuario no puede exceder 50 caracteres")
    private String username;

    private String photoUrl;

    @Size(max = 500, message = "La biografía no puede exceder 500 caracteres")
    private String biography;

    // ===== Campos para bibliotecarios LIBRARIAN =====
    @Size(max = 150, message = "El nombre de la biblioteca no puede exceder 150 caracteres")
    private String libraryName;

    private String libraryLocation; // address

    @Email(message = "El correo de contacto no tiene un formato válido")
    private String contactInfo; // contactEmail

    private String contactPhone;

    private String officeHours; // openingHours

    private Long idZone; // Para actualizar la zona/comuna

    // ===== Getters y Setters =====
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    public String getLibraryLocation() {
        return libraryLocation;
    }

    public void setLibraryLocation(String libraryLocation) {
        this.libraryLocation = libraryLocation;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getOfficeHours() {
        return officeHours;
    }

    public void setOfficeHours(String officeHours) {
        this.officeHours = officeHours;
    }

    public Long getIdZone() {
        return idZone;
    }

    public void setIdZone(Long idZone) {
        this.idZone = idZone;
    }
}
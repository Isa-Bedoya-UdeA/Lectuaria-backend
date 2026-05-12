package com.lectuaria.backend.dto.library;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LibraryRequestDTO {

    @NotBlank(message = "El nombre de la biblioteca es obligatorio.")
    private String name;

    private String description;

    @NotBlank(message = "La dirección de la biblioteca es obligatoria.")
    private String address;

    @NotBlank(message = "El correo de contacto es obligatorio.")
    private String contactEmail;

    private String contactPhone;

    @NotBlank(message = "El horario de atención es obligatorio.")
    private String openingHours;

    @NotNull(message = "La zona/comuna es obligatoria.")
    private Long idZone; // ID de LIVING_ZONE (Medellín)

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public Long getIdZone() {
        return idZone;
    }

    public void setIdZone(Long idZone) {
        this.idZone = idZone;
    }
}
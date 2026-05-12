package com.lectuaria.backend.dto.book;

import jakarta.validation.constraints.AssertTrue;

public class AvailabilityDTO {
    private boolean physical;
    private boolean digital;
    private Integer physicalCopies;
    private String digitalPlatform;

    @AssertTrue(message = "Al menos un tipo de disponibilidad debe ser seleccionado.")
    public boolean isAvailable() {
        return physical || digital;
    }

    // Getters y Setters
    public boolean isPhysical() {
        return physical;
    }

    public void setPhysical(boolean physical) {
        this.physical = physical;
    }

    public boolean isDigital() {
        return digital;
    }

    public void setDigital(boolean digital) {
        this.digital = digital;
    }

    public Integer getPhysicalCopies() {
        return physicalCopies;
    }

    public void setPhysicalCopies(Integer physicalCopies) {
        this.physicalCopies = physicalCopies;
    }

    public String getDigitalPlatform() {
        return digitalPlatform;
    }

    public void setDigitalPlatform(String digitalPlatform) {
        this.digitalPlatform = digitalPlatform;
    }
}
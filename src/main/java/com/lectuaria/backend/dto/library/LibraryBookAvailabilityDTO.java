package com.lectuaria.backend.dto.library;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LibraryBookAvailabilityDTO {

    private Integer physicalCopies;
    private Boolean digitalAvailable;
    private Long digitalPlatformId;

    public LibraryBookAvailabilityDTO() {}

    public LibraryBookAvailabilityDTO(Integer physicalCopies, Boolean digitalAvailable, Long digitalPlatformId) {
        this.physicalCopies = physicalCopies;
        this.digitalAvailable = digitalAvailable;
        this.digitalPlatformId = digitalPlatformId;
    }

    public Integer getPhysicalCopies() {
        return physicalCopies;
    }

    public void setPhysicalCopies(Integer physicalCopies) {
        this.physicalCopies = physicalCopies;
    }

    public Boolean getDigitalAvailable() {
        return digitalAvailable;
    }

    public void setDigitalAvailable(Boolean digitalAvailable) {
        this.digitalAvailable = digitalAvailable;
    }

    public Long getDigitalPlatformId() {
        return digitalPlatformId;
    }

    public void setDigitalPlatformId(Long digitalPlatformId) {
        this.digitalPlatformId = digitalPlatformId;
    }
}
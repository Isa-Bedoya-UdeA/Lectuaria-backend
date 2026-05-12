package com.lectuaria.backend.dto.library;

public class LibraryAvailabilityDTO {
    private LibrarySummaryDTO library;
    private boolean physicalAvailable;
    private Integer physicalCopies;
    private boolean digitalAvailable;
    private String digitalPlatform;

    public LibraryAvailabilityDTO() {}

    public LibraryAvailabilityDTO(LibrarySummaryDTO library, boolean physicalAvailable, Integer physicalCopies, boolean digitalAvailable, String digitalPlatform) {
        this.library = library;
        this.physicalAvailable = physicalAvailable;
        this.physicalCopies = physicalCopies;
        this.digitalAvailable = digitalAvailable;
        this.digitalPlatform = digitalPlatform;
    }

    // Getters and Setters
    public LibrarySummaryDTO getLibrary() { return library; }
    public void setLibrary(LibrarySummaryDTO library) { this.library = library; }

    public boolean isPhysicalAvailable() { return physicalAvailable; }
    public void setPhysicalAvailable(boolean physicalAvailable) { this.physicalAvailable = physicalAvailable; }

    public Integer getPhysicalCopies() { return physicalCopies; }
    public void setPhysicalCopies(Integer physicalCopies) { this.physicalCopies = physicalCopies; }

    public boolean isDigitalAvailable() { return digitalAvailable; }
    public void setDigitalAvailable(boolean digitalAvailable) { this.digitalAvailable = digitalAvailable; }

    public String getDigitalPlatform() { return digitalPlatform; }
    public void setDigitalPlatform(String digitalPlatform) { this.digitalPlatform = digitalPlatform; }
}

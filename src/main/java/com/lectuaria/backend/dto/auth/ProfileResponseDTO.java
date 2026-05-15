package com.lectuaria.backend.dto.auth;

public class ProfileResponseDTO {
    // ========== CAMPOS COMUNES PARA TODOS LOS USUARIOS ==========
    private final Long id;
    private final String email;
    private final String fullName;
    private final String userRole;
    private final String username;
    private final String photoUrl;
    private final String biography;

    // ========== CAMPOS ESPECÍFICOS PARA BIBLIOTECARIOS ==========
    // (Serán null para usuarios lectores)
    private final String libraryName;
    private final String libraryAddress;
    private final String libraryContactEmail;
    private final String libraryContactPhone;
    private final String libraryOpeningHours;
    private final Long libraryZoneId;
    private final String libraryZoneName;
    private Long libraryId;

    // Constructor para usuarios LECTORES
    public ProfileResponseDTO(Long id, String email, String fullName, String userRole, String username,
            String photoUrl, String biography) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.userRole = userRole;
        this.username = username;
        this.photoUrl = photoUrl;
        this.biography = biography;
        // Campos de biblioteca = null
        this.libraryName = null;
        this.libraryAddress = null;
        this.libraryContactEmail = null;
        this.libraryContactPhone = null;
        this.libraryOpeningHours = null;
        this.libraryZoneId = null;
        this.libraryZoneName = null;
        this.libraryId = null;
    }

    public ProfileResponseDTO(Long id, String email, String fullName, String userRole, String username,
            String photoUrl, String biography,
            String libraryName, String libraryAddress, String libraryContactEmail,
            String libraryContactPhone, String libraryOpeningHours,
            Long libraryZoneId, String libraryZoneName, Long libraryId) {

        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.userRole = userRole;
        this.username = username;
        this.photoUrl = photoUrl;
        this.biography = biography;
        this.libraryName = libraryName;
        this.libraryAddress = libraryAddress;
        this.libraryContactEmail = libraryContactEmail;
        this.libraryContactPhone = libraryContactPhone;
        this.libraryOpeningHours = libraryOpeningHours;
        this.libraryZoneId = libraryZoneId;
        this.libraryZoneName = libraryZoneName;
        this.libraryId = libraryId;
    }

    // ===== Getters comunes =====
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUserRole() {
        return userRole;
    }

    public String getUsername() {
        return username;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getBiography() {
        return biography;
    }

    // ===== Getters específicos de biblioteca =====
    public String getLibraryName() {
        return libraryName;
    }

    public String getLibraryAddress() {
        return libraryAddress;
    }

    public String getLibraryContactEmail() {
        return libraryContactEmail;
    }

    public String getLibraryContactPhone() {
        return libraryContactPhone;
    }

    public String getLibraryOpeningHours() {
        return libraryOpeningHours;
    }

    public Long getLibraryZoneId() {
        return libraryZoneId;
    }

    public String getLibraryZoneName() {
        return libraryZoneName;
    }

    public void setLibraryId(Long libraryId) {
        this.libraryId = libraryId;
    }

    public Long getLibraryId() {
        return libraryId;
    }
}
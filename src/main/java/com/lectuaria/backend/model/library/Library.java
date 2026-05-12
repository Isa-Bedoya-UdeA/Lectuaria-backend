package com.lectuaria.backend.model.library;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "libraries")
public class Library {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_library")
    private Long id;

    @Column(name = "name_library", nullable = false)
    private String name;

    @Column(name = "library_description")
    private String description;

    @Column(nullable = false)
    private String address;

    @Column(name = "contact_email", unique = true, nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "opening_hours")
    private String openingHours;

    @Column(name = "id_zone", nullable = false)
    private Long idZone; // FK a LIVING_ZONE

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Library() {
    }

    public Library(String name, String description, String address, String contactEmail,
            String contactPhone, String openingHours, Long idZone) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.openingHours = openingHours;
        this.idZone = idZone;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
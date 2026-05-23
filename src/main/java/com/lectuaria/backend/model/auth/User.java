package com.lectuaria.backend.model.auth;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "app_user")
public class User {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_user", nullable = false)
    private UserRole role;

    @Column(name = "user_name", nullable = false, unique = true)
    private String username;

    @Column(name = "profile_photo")
    private String photoUrl;

    private String biography;

    @CreationTimestamp
    @Column(name = "register_date", updatable = false)
    private Instant createdAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    public User() {
    }

    public User(
            String fullName,
            String email,
            String passwordHash,
            UserRole role,
            String username,
            String photoUrl,
            String biography) {

        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.username = username;
        this.photoUrl = photoUrl;
        this.biography = biography;
    }

    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public void recordFailedLogin() {
        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60);
        }
    }

    public void resetFailedLoginAttempts() {
        failedAttempts = 0;
        lockedUntil = null;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
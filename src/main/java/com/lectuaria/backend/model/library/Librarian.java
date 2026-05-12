package com.lectuaria.backend.model.library;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import com.lectuaria.backend.model.auth.User;

import java.time.Instant;

@Entity
@Table(name = "librarian")
public class Librarian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_librarian")
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_user", nullable = false, unique = true)
    private User user;

    @OneToOne
    @JoinColumn(name = "id_library", nullable = false, unique = true)
    private Library library;

    @Column(name = "library_email", unique = true, nullable = false)
    private String libraryEmail;

    @Column(nullable = false)
    private Boolean approved = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Librarian() {
    }

    public Librarian(User user, Library library, String libraryEmail) {
        this.user = user;
        this.library = library;
        this.libraryEmail = libraryEmail;
        this.approved = false;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Library getLibrary() {
        return library;
    }

    public void setLibrary(Library library) {
        this.library = library;
    }

    public String getLibraryEmail() {
        return libraryEmail;
    }

    public void setLibraryEmail(String libraryEmail) {
        this.libraryEmail = libraryEmail;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

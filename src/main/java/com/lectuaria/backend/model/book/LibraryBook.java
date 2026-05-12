package com.lectuaria.backend.model.book;

import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.library.Library;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "library_book")
public class LibraryBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_library_book")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_library", nullable = false)
    private Library library;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_book", nullable = false)
    private Book book;

    @Column(name = "physical_copies", columnDefinition = "INTEGER DEFAULT 0")
    private Integer physicalCopies = 0;

    @Column(name = "digital_available", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean digitalAvailable = false;

    @Column(name = "digital_platform", length = 100)
    private String digitalPlatform;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user_added")
    private User userAdded;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private Instant addedAt;

    // Constructores
    public LibraryBook() {
    }

    public LibraryBook(Library library, Book book, Integer physicalCopies,
            Boolean digitalAvailable, String digitalPlatform, User userAdded) {
        this.library = library;
        this.book = book;
        this.physicalCopies = physicalCopies != null ? physicalCopies : 0;
        this.digitalAvailable = digitalAvailable != null ? digitalAvailable : false;
        this.digitalPlatform = digitalPlatform;
        this.userAdded = userAdded;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public Library getLibrary() {
        return library;
    }

    public void setLibrary(Library library) {
        this.library = library;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
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

    public String getDigitalPlatform() {
        return digitalPlatform;
    }

    public void setDigitalPlatform(String digitalPlatform) {
        this.digitalPlatform = digitalPlatform;
    }

    public User getUserAdded() {
        return userAdded;
    }

    public void setUserAdded(User userAdded) {
        this.userAdded = userAdded;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
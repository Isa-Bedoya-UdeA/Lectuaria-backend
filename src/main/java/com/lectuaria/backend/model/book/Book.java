package com.lectuaria.backend.model.book;

import com.lectuaria.backend.model.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_book")
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(unique = true, nullable = false)
    private Long isbn;

    @Column(name = "number_of_pages")
    private Integer pages;

    @Column(name = "ratings_count")
    private Integer ratingsCount;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "front_page_url")
    private String coverUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "creation_date", updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user_created")
    private User createdBy;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;
    @ManyToMany
    @JoinTable(name = "book_author", joinColumns = @JoinColumn(name = "id_book"), inverseJoinColumns = @JoinColumn(name = "id_author"))
    private List<Author> authors;

    @ManyToMany
    @JoinTable(name = "book_genre", joinColumns = @JoinColumn(name = "id_book"), inverseJoinColumns = @JoinColumn(name = "id_genre"))
    private List<Genre> genres;

    @ManyToMany
    @JoinTable(name = "book_publisher", joinColumns = @JoinColumn(name = "id_book"), inverseJoinColumns = @JoinColumn(name = "id_publisher"))
    private List<Publisher> publishers;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LibraryBook> libraryBooks;

    // Constructores, Getters y Setters
    public Book() {
    }

    public List<LibraryBook> getLibraryBooks() {
        return libraryBooks;
    }

    public void setLibraryBooks(List<LibraryBook> libraryBooks) {
        this.libraryBooks = libraryBooks;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getIsbn() {
        return isbn;
    }

    public void setIsbn(Long isbn) {
        this.isbn = isbn;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getRatingsCount() {
        return ratingsCount;
    }

    public void setRatingsCount(Integer ratingsCount) {
        this.ratingsCount = ratingsCount;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(List<Author> authors) {
        this.authors = authors;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }

    public List<Publisher> getPublishers() {
        return publishers;
    }

    public void setPublishers(List<Publisher> publishers) {
        this.publishers = publishers;
    }

    
}
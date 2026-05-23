package com.lectuaria.backend.dto.book;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class BookSummaryDTO {
    private Long id;
    private Long isbn;
    private String title;
    private List<String> authors;
    private List<GenreDTO> genres;
    private BigDecimal averageRating;
    private Integer ratingsCount;
    private String coverUrl;
    private Long libraryId;
    private Long userAddedId;
    private Long createdById;
    private List<String> availableLibraries;
    private Instant createdAt;

    public BookSummaryDTO() {}

    public BookSummaryDTO(Long id, Long isbn, String title, List<String> authors,
            List<GenreDTO> genres, BigDecimal averageRating,
            Integer ratingsCount, String coverUrl, Long libraryId, Long userAddedId, Long createdById,
            Instant createdAt) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.authors = authors;
        this.genres = genres;
        this.averageRating = averageRating;
        this.ratingsCount = ratingsCount;
        this.coverUrl = coverUrl;
        this.libraryId = libraryId;
        this.userAddedId = userAddedId;
        this.createdById = createdById;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIsbn() { return isbn; }
    public void setIsbn(Long isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getAuthors() { return authors; }
    public void setAuthors(List<String> authors) { this.authors = authors; }

    public List<GenreDTO> getGenres() { return genres; }
    public void setGenres(List<GenreDTO> genres) { this.genres = genres; }

    public BigDecimal getAverageRating() { return averageRating; }
    public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }

    public Integer getRatingsCount() { return ratingsCount; }
    public void setRatingsCount(Integer ratingsCount) { this.ratingsCount = ratingsCount; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public Long getLibraryId() { return libraryId; }
    public void setLibraryId(Long libraryId) { this.libraryId = libraryId; }

    public Long getUserAddedId() { return userAddedId; }
    public void setUserAddedId(Long userAddedId) { this.userAddedId = userAddedId; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public List<String> getAvailableLibraries() { return availableLibraries; }
    public void setAvailableLibraries(List<String> availableLibraries) { this.availableLibraries = availableLibraries; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
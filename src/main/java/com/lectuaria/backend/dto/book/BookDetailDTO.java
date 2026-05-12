package com.lectuaria.backend.dto.book;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.lectuaria.backend.dto.library.LibraryAvailabilityDTO;

public class BookDetailDTO {
    private Long id;
    private String title;
    private List<String> authors;
    private List<GenreDTO> genres;
    private BigDecimal averageRating;
    private Integer ratingsCount;
    private String coverUrl;
    private String description;
    private List<String> publishers;
    private LocalDate publicationDate;
    private Integer pages;
    private Long isbn;
    private List<String> formats;
    private List<LibraryAvailabilityDTO> availability;

    public BookDetailDTO() {}

    public BookDetailDTO(Long id, String title, List<String> authors,
            List<GenreDTO> genres, BigDecimal averageRating,
            Integer ratingsCount, String coverUrl,
            String description, List<String> publishers,
            LocalDate publicationDate, Integer pages,
            Long isbn, List<String> formats) {
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.genres = genres;
        this.averageRating = averageRating;
        this.ratingsCount = ratingsCount;
        this.coverUrl = coverUrl;
        this.description = description;
        this.publishers = publishers;
        this.publicationDate = publicationDate;
        this.pages = pages;
        this.isbn = isbn;
        this.formats = formats;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getPublishers() { return publishers; }
    public void setPublishers(List<String> publishers) { this.publishers = publishers; }

    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }

    public Integer getPages() { return pages; }
    public void setPages(Integer pages) { this.pages = pages; }

    public Long getIsbn() { return isbn; }
    public void setIsbn(Long isbn) { this.isbn = isbn; }

    public List<String> getFormats() { return formats; }
    public void setFormats(List<String> formats) { this.formats = formats; }

    public List<LibraryAvailabilityDTO> getAvailability() { return availability; }
    public void setAvailability(List<LibraryAvailabilityDTO> availability) { this.availability = availability; }
}
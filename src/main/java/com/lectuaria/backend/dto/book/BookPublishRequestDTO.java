package com.lectuaria.backend.dto.book;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public class BookPublishRequestDTO {
    @NotNull(message = "El ISBN es obligatorio.")
    private Long isbn;

    @NotBlank(message = "El título es obligatorio.")
    @Size(max = 255, message = "El título no puede exceder 255 caracteres.")
    private String title;

    @NotEmpty(message = "Al menos un autor es obligatorio.")
    private List<String> authors;

    private String description;

    private List<String> genres;

    private String coverUrl;

    private List<String> publishers;

    private LocalDate publicationDate;

    @Min(value = 1, message = "El número de páginas debe ser mayor a 0.")
    private Integer pages;

    @NotNull(message = "La disponibilidad es obligatoria.")
    private AvailabilityDTO availability;

    @NotNull(message = "El ID de la biblioteca es obligatorio.")
    private Long libraryId;

    private boolean bookExistsInCatalog;
    private boolean bookExistsInUserLibrary;

    /**
     * Modalidad de disponibilidad del libro en el catálogo global.
     * Valores: "physical" | "digital" | "both"
     * Mapea a registros en la tabla book_format (Físico / Digital).
     * Por defecto "both" si no se especifica.
     */
    private String availabilityMode;

    /**
     * ID de la plataforma digital (Kindle, Kobo, Google Books, etc.)
     * Solo aplica cuando availabilityMode es "digital" o "both".
     */
    private Long platformId;

    // Getters y Setters
    public boolean getBookExistsInCatalog() {
        return bookExistsInCatalog;
    }

    public void setBookExistsInCatalog(boolean bookExistsInCatalog) {
        this.bookExistsInCatalog = bookExistsInCatalog;
    }

    public boolean getBookExistsInUserLibrary() {
        return bookExistsInUserLibrary;
    }

    public void setBookExistsInUserLibrary(boolean bookExistsInUserLibrary) {
        this.bookExistsInUserLibrary = bookExistsInUserLibrary;
    }

    public Long getIsbn() {
        return isbn;
    }

    public void setIsbn(Long isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public List<String> getPublishers() {
        return publishers;
    }

    public void setPublishers(List<String> publishers) {
        this.publishers = publishers;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public AvailabilityDTO getAvailability() {
        return availability;
    }

    public void setAvailability(AvailabilityDTO availability) {
        this.availability = availability;
    }

    public Long getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(Long libraryId) {
        this.libraryId = libraryId;
    }

    public String getAvailabilityMode() {
        return availabilityMode;
    }

    public void setAvailabilityMode(String availabilityMode) {
        this.availabilityMode = availabilityMode;
    }

    public Long getPlatformId() {
        return platformId;
    }

    public void setPlatformId(Long platformId) {
        this.platformId = platformId;
    }
}

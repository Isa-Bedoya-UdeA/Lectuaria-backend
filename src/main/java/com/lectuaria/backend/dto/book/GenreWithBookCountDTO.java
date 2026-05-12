package com.lectuaria.backend.dto.book;

public class GenreWithBookCountDTO {
    private final Long id;
    private final String name;
    private final String description;
    private final Long bookCount;

    public GenreWithBookCountDTO(Long id, String name, String description, Long bookCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.bookCount = bookCount;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getBookCount() {
        return bookCount;
    }

    public boolean hasBooks() {
        return bookCount != null && bookCount > 0;
    }
}

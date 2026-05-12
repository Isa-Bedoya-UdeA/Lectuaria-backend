package com.lectuaria.backend.dto.book;

public class GenreDTO {
    private final Long id;
    private final String name;
    private final String description; // Para hover en frontend

    public GenreDTO(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
}
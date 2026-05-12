package com.lectuaria.backend.model.book;

import jakarta.persistence.*;

@Entity
@Table(name = "format")
public class Format {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_format")
    private Long id;

    @Column(name = "format_name", unique = true, nullable = false, length = 100)
    private String name;

    public Format() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
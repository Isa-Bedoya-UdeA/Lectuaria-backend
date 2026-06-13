package com.lectuaria.backend.model.book;

import jakarta.persistence.*;

@Entity
@Table(name = "author")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_author")
    private Long id;

    @Column(name = "author_name", unique = true, nullable = false, length = 150)
    private String name;

    // Constructor sin argumentos requerido por JPA (Jakarta Persistence)
    // para instanciar la entidad via reflection al cargar desde la BD.
    public Author() {
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
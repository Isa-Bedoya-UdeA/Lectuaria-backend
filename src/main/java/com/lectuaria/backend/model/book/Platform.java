package com.lectuaria.backend.model.book;

import jakarta.persistence.*;

@Entity
@Table(name = "platform")
public class Platform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_platform")
    private Long id;

    @Column(name = "name_platform", unique = true, nullable = false, length = 100)
    private String name;

    // Constructor sin argumentos requerido por JPA (Jakarta Persistence)
    // para instanciar la entidad via reflection al cargar desde la BD.
    public Platform() {
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
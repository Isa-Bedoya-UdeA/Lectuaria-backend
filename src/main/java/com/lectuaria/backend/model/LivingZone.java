package com.lectuaria.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "living_zone")
public class LivingZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_zone")
    private Long id;

    @Column(name = "name_zone", unique = true, nullable = false)
    private String name;

    public LivingZone() {
    }

    public LivingZone(String name) {
        this.name = name;
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
package com.lectuaria.backend.repository.library;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lectuaria.backend.model.library.Library;

import java.util.Optional;

public interface LibraryRepository extends JpaRepository<Library, Long> {

    Optional<Library> findByContactEmail(String contactEmail);

    boolean existsByContactEmail(String contactEmail);

    // Buscar bibliotecas por zona (comuna de Medellín)
    java.util.List<Library> findByIdZone(Long idZone);
}
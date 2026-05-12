package com.lectuaria.backend.repository.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lectuaria.backend.model.book.Format;

import java.util.Optional;

@Repository
public interface FormatRepository extends JpaRepository<Format, Long> {
    Optional<Format> findByName(String name);

    boolean existsByName(String name);
}
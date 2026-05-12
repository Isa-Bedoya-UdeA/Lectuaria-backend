package com.lectuaria.backend.repository.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformRepository extends JpaRepository<com.lectuaria.backend.model.book.Platform, Long> {
    Optional<com.lectuaria.backend.model.book.Platform> findByName(String name);

    boolean existsByName(String name);
}

package com.lectuaria.backend.repository.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lectuaria.backend.model.book.Publisher;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, Long> {
    Optional<Publisher> findByName(String name);

    boolean existsByName(String name);

    List<Publisher> findByNameContainingIgnoreCase(String name);
}
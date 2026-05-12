package com.lectuaria.backend.repository.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lectuaria.backend.model.book.Author;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByName(String name);

    boolean existsByName(String name);

    List<Author> findByNameContainingIgnoreCase(String name);
}
package com.lectuaria.backend.repository.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lectuaria.backend.model.book.Genre;
import com.lectuaria.backend.dto.book.GenreWithBookCountDTO;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByName(String name);

    boolean existsByName(String name);

    List<Genre> findByNameContainingIgnoreCase(String name);

    @Query("SELECT new com.lectuaria.backend.dto.book.GenreWithBookCountDTO(g.id, g.name, g.description, COUNT(b.id)) " +
           "FROM Genre g " +
           "JOIN g.books b " +
           "GROUP BY g.id, g.name, g.description " +
           "ORDER BY COUNT(b.id) DESC, g.name ASC")
    List<GenreWithBookCountDTO> findAllWithBookCount();
}
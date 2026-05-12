package com.lectuaria.backend.repository.library;

import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.library.Librarian;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LibrarianRepository extends JpaRepository<Librarian, Long> {

    Optional<Librarian> findByUser(User user);

    Optional<Librarian> findByUserId(Long userId);

    Optional<Librarian> findByLibraryEmail(String libraryEmail);

    boolean existsByUser(User user);
}
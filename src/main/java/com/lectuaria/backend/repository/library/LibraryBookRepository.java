package com.lectuaria.backend.repository.library;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lectuaria.backend.model.book.LibraryBook;
import java.util.Optional;

@Repository
public interface LibraryBookRepository extends JpaRepository<LibraryBook, Long> {

    boolean existsByLibraryIdAndBookId(Long libraryId, Long bookId);

    Optional<LibraryBook> findByLibraryIdAndBookId(Long libraryId, Long bookId);

    // Opcional: buscar por ISBN en lugar de ID de libro
    @Query("SELECT CASE WHEN COUNT(lb) > 0 THEN true ELSE false END " +
            "FROM LibraryBook lb " +
            "JOIN lb.book b " +
            "WHERE lb.library.id = :libraryId AND b.isbn = :isbn")
    boolean existsByLibraryIdAndBookIsbn(@Param("libraryId") Long libraryId, @Param("isbn") String isbn);

    // Método para buscar libros por biblioteca
    Page<LibraryBook> findByLibraryId(Long libraryId, Pageable pageable);

    // Método para contar cuántas bibliotecas tienen un libro
    long countByBookId(Long bookId);
}
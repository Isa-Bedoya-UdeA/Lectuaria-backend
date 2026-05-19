package com.lectuaria.backend.repository.library;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lectuaria.backend.model.book.LibraryBook;
import java.util.Optional;
import java.time.Instant;
import java.util.List;

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

    long countByLibraryId(Long libraryId);

    long countByLibraryIdAndAddedAtGreaterThanEqual(Long libraryId, Instant from);

    @Query("SELECT g.id, g.name, COUNT(lb) FROM LibraryBook lb JOIN lb.book.genres g " +
            "WHERE lb.library.id = :libraryId GROUP BY g.id, g.name ORDER BY COUNT(lb) DESC, g.name ASC")
    List<Object[]> findTopGenresByLibraryId(@Param("libraryId") Long libraryId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM BookReview r WHERE r.status = com.lectuaria.backend.model.book.ReviewStatus.published " +
            "AND r.book.id IN (SELECT lb.book.id FROM LibraryBook lb WHERE lb.library.id = :libraryId)")
    long countPublishedReviewsByLibraryId(@Param("libraryId") Long libraryId);

    @Query("SELECT COALESCE(AVG(lb.book.averageRating), 0) FROM LibraryBook lb " +
            "WHERE lb.library.id = :libraryId AND lb.book.averageRating IS NOT NULL")
    java.math.BigDecimal calculateAverageRatingByLibraryId(@Param("libraryId") Long libraryId);

    @Query("SELECT lb FROM LibraryBook lb WHERE lb.library.id = :libraryId " +
            "ORDER BY COALESCE(lb.book.ratingsCount, 0) DESC, lb.book.title ASC")
    List<LibraryBook> findMostPopularByLibraryId(@Param("libraryId") Long libraryId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM BookReview r WHERE r.book.id = :bookId AND r.status = com.lectuaria.backend.model.book.ReviewStatus.published")
    long countPublishedReviewsByBookId(@Param("bookId") Long bookId);
}

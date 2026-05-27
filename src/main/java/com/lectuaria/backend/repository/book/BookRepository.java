package com.lectuaria.backend.repository.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lectuaria.backend.model.book.Book;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

	Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

	Optional<Book> findByIsbn(Long isbn);

	@Query("SELECT DISTINCT b FROM Book b " +
			"LEFT JOIN b.authors a " +
			"LEFT JOIN b.genres g " +
			"WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
			"   OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
			"   OR LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	Page<Book> searchBooks(@Param("keyword") String keyword, Pageable pageable);

	@Query("SELECT b FROM Book b JOIN b.genres g WHERE g.id = :genreId")
	Page<Book> findByGenreId(@Param("genreId") Long genreId, Pageable pageable);

	@Query("SELECT DISTINCT b FROM Book b " +
			"JOIN b.genres g " +
			"WHERE g.id IN :genreIds")
	Page<Book> findByGenreIdsIn(@Param("genreIds") List<Long> genreIds, Pageable pageable);

	@Query("SELECT b FROM Book b JOIN b.authors a WHERE a.id = :authorId")
	Page<Book> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

	@Query("SELECT b FROM Book b ORDER BY b.ratingsCount DESC")
	Page<Book> findMostPopular(Pageable pageable);

	@Query("SELECT b FROM Book b WHERE b.averageRating IS NOT NULL ORDER BY b.averageRating DESC")
	Page<Book> findTopRated(Pageable pageable);

	@Query("SELECT DISTINCT b FROM Book b " +
			"LEFT JOIN b.genres g " +
			"WHERE b.createdAt >= :since " +
			"AND (:genreId IS NULL OR g.id = :genreId) " +
			"ORDER BY b.createdAt DESC")
	Page<Book> findNewCatalogBooks(@Param("since") Instant since, @Param("genreId") Long genreId, Pageable pageable);

	@Query("SELECT DISTINCT b FROM Book b LEFT JOIN b.genres g " +
			"WHERE b.averageRating >= 4.0 AND b.ratingsCount >= 10 " +
			"AND (:genreId IS NULL OR g.id = :genreId) " +
			"AND (:year IS NULL OR YEAR(b.publicationDate) = :year) " +
			"ORDER BY b.averageRating DESC, b.ratingsCount DESC")
	Page<Book> findQualifiedTopRated(@Param("genreId") Long genreId, @Param("year") Integer year, Pageable pageable);

	@Query("SELECT DISTINCT b FROM Book b JOIN b.genres g WHERE g.id IN :genreIds AND b.id <> :bookId")
	List<Book> findSimilarByGenreIds(@Param("bookId") Long bookId, @Param("genreIds") List<Long> genreIds, Pageable pageable);

	@Query("SELECT DISTINCT b FROM Book b JOIN b.authors a WHERE a.id IN :authorIds AND b.id <> :bookId")
	List<Book> findSimilarByAuthorIds(@Param("bookId") Long bookId, @Param("authorIds") List<Long> authorIds, Pageable pageable);

	@Query("SELECT DISTINCT b FROM Book b JOIN b.genres g WHERE g.id IN :genreIds " +
			"ORDER BY b.averageRating DESC, b.ratingsCount DESC, b.createdAt DESC")
	List<Book> findRecommendationsByGenreIds(@Param("genreIds") List<Long> genreIds, Pageable pageable);

	@Query("SELECT b FROM Book b ORDER BY b.averageRating DESC, b.ratingsCount DESC, b.createdAt DESC")
	List<Book> findFallbackRecommendations(Pageable pageable);

	boolean existsByIsbn(Long isbn);

	@Query("SELECT lb.book FROM LibraryBook lb WHERE lb.library.id = :libraryId")
	Page<Book> findByLibraryId(@Param("libraryId") Long libraryId, Pageable pageable);

	@Query("SELECT DISTINCT lb.book FROM LibraryBook lb " +
			"WHERE (:formatType = 'physical' AND lb.physicalCopies > 0) " +
			"OR (:formatType = 'digital' AND lb.digitalAvailable = true)")
	Page<Book> findByFormatAvailability(@Param("formatType") String formatType, Pageable pageable);

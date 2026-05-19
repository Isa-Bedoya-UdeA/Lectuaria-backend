package com.lectuaria.backend.repository.book;

import com.lectuaria.backend.model.book.BookRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

@Repository
public interface BookRatingRepository extends JpaRepository<BookRating, Long> {

    Optional<BookRating> findByBookIdAndUserId(Long bookId, Long userId);

    List<BookRating> findByBookId(Long bookId);

    Page<BookRating> findByBookId(Long bookId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(br.rating), 0) FROM BookRating br WHERE br.book.id = :bookId")
    BigDecimal calculateAverageRatingByBookId(@Param("bookId") Long bookId);

    Page<BookRating> findByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    long countByBookId(Long bookId);

    long countDistinctByUserId(Long userId);

    @Query("SELECT COALESCE(AVG(br.rating), 0) FROM BookRating br WHERE br.user.id = :userId")
    BigDecimal calculateAverageRatingByUserId(@Param("userId") Long userId);

    @Query("SELECT MONTH(br.createdAt), COUNT(DISTINCT br.book.id) FROM BookRating br " +
            "WHERE br.user.id = :userId AND br.createdAt >= :from AND br.createdAt < :to " +
            "GROUP BY MONTH(br.createdAt) ORDER BY MONTH(br.createdAt)")
    List<Object[]> countBooksReadByMonth(@Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT COUNT(DISTINCT br.book.id) FROM BookRating br WHERE br.user.id = :userId " +
            "AND br.createdAt >= :from AND br.createdAt < :to")
    long countDistinctBooksByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT g.id, g.name, COUNT(br) FROM BookRating br JOIN br.book.genres g " +
            "WHERE br.user.id = :userId GROUP BY g.id, g.name ORDER BY COUNT(br) DESC, g.name ASC")
    List<Object[]> findTopGenresByUserRatings(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT DISTINCT br.book.id FROM BookRating br WHERE br.user.id = :userId")
    List<Long> findRatedBookIdsByUserId(@Param("userId") Long userId);
}

package com.lectuaria.backend.repository.book;

import com.lectuaria.backend.model.book.BookRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRatingRepository extends JpaRepository<BookRating, Long> {

    Optional<BookRating> findByBookIdAndUserId(Long bookId, Long userId);

    List<BookRating> findByBookId(Long bookId);

    @Query("SELECT COALESCE(AVG(br.rating), 0) FROM BookRating br WHERE br.book.id = :bookId")
    BigDecimal calculateAverageRatingByBookId(Long bookId);

    Page<BookRating> findByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    long countByBookId(Long bookId);
}

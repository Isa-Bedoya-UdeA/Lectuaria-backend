package com.lectuaria.backend.repository.book;

import com.lectuaria.backend.model.book.BookRatingStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRatingStatsRepository extends JpaRepository<BookRatingStats, Long> {

    Optional<BookRatingStats> findByBookId(Long bookId);

    void deleteByBookId(Long bookId);
}

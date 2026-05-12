package com.lectuaria.backend.repository.book;

import com.lectuaria.backend.model.book.BookReview;
import com.lectuaria.backend.model.book.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Long> {
    Optional<BookReview> findByBookIdAndUserId(Long bookId, Long userId);

    Page<BookReview> findByBookIdAndStatusOrderByPublishedAtDesc(Long bookId, ReviewStatus status, Pageable pageable);

    @Query("SELECT r FROM BookReview r WHERE r.user.id IN :userIds AND r.status = :status ORDER BY r.publishedAt DESC")
    List<BookReview> findRecentByUserIdsAndStatus(@Param("userIds") List<Long> userIds, @Param("status") ReviewStatus status, Pageable pageable);
}

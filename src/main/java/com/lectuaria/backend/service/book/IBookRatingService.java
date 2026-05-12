package com.lectuaria.backend.service.book;

import com.lectuaria.backend.dto.book.BookRatingResponseDTO;
import com.lectuaria.backend.dto.book.BookRatingWithUserDTO;
import com.lectuaria.backend.dto.book.BookReviewPreviewRequestDTO;
import com.lectuaria.backend.dto.book.BookReviewPreviewResponseDTO;
import com.lectuaria.backend.dto.book.BookReviewResponseDTO;
import com.lectuaria.backend.dto.book.BookReviewUpsertRequestDTO;
import com.lectuaria.backend.dto.common.PaginatedResponse;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.BookRatingStats;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.List;

public interface IBookRatingService {
    BookRatingResponseDTO rateBook(@NonNull Long bookId, @NonNull User user, @NonNull BigDecimal rating);
    BookRatingResponseDTO getBookRating(@NonNull Long bookId, @NonNull User user);
    List<BookRatingWithUserDTO> getAllBookRatings(@NonNull Long bookId);
    BookRatingResponseDTO updateRating(@NonNull Long ratingId, @NonNull BigDecimal newRating, @NonNull User user);
    void deleteRating(@NonNull Long ratingId, @NonNull User user);
    void deleteRatingByBookAndUser(@NonNull Long bookId, @NonNull User user);
    BookReviewResponseDTO saveReview(@NonNull Long bookId, @NonNull User user, @NonNull BookReviewUpsertRequestDTO request);
    BookReviewPreviewResponseDTO previewReview(@NonNull Long bookId, @NonNull BookReviewPreviewRequestDTO request);
    PaginatedResponse<BookReviewResponseDTO> getPublishedReviews(@NonNull Long bookId, int page, int size);
    BookReviewResponseDTO updateReview(@NonNull Long reviewId, @NonNull User user, @NonNull BookReviewUpsertRequestDTO request);
    void deleteReview(@NonNull Long reviewId, @NonNull User user);
    void refreshBookAggregates(Book book);
    BookRatingStats getBookRatingStats(@NonNull Long bookId);
    void syncBookRatingStatsFromBook(Book book);
}

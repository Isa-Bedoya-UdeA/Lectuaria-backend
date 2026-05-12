package com.lectuaria.backend.service.book;

import com.lectuaria.backend.dto.book.*;
import com.lectuaria.backend.dto.common.PaginatedResponse;
import com.lectuaria.backend.model.book.Book;

import java.util.List;

public interface IBookService {
    PaginatedResponse<BookSummaryDTO> searchBooks(String keyword, int page, int size);
    PaginatedResponse<BookSummaryDTO> getAllBooks(int page, int size, Float minRating, Integer startYear, Integer endYear, List<String> formatTypes, Long userId);
    PaginatedResponse<BookSummaryDTO> getBooksByGenre(Long genreId, int page, int size);
    PaginatedResponse<BookSummaryDTO> getBooksByLibrary(Long libraryId, int page, int size);
    PaginatedResponse<BookSummaryDTO> getBooksByGenres(List<Long> genreIds, int page, int size);
    PaginatedResponse<BookSummaryDTO> getBooksByGenresWithLibraryInfo(List<Long> genreIds, int page, int size, Long userId);
    PaginatedResponse<BookSummaryDTO> getBooksByAuthor(Long authorId, int page, int size);
    PaginatedResponse<BookSummaryDTO> getMostPopular(int page, int size);
    PaginatedResponse<BookSummaryDTO> getTopRated(int page, int size, Long genreId, Integer year);
    PaginatedResponse<BookCatalogItemDTO> getNewCatalogBooks(int page, int size, Long genreId, String formatName);
    FeaturedSectionsDTO getFeaturedSections();
    List<BookSummaryDTO> getSimilarBooks(Long bookId);
    PaginatedResponse<BookSummaryDTO> getMostPopularWithLibraryInfo(int page, int size, Long userId);
    PaginatedResponse<BookSummaryDTO> searchBooksByKeywordsAndLibraries(List<String> keywords, List<Long> libraryIds, int page, int size);
    BookDetailDTO getBookById(Long id);
    BookDetailDTO getBookByIsbn(Long isbn);
    void removeBookFromLibrary(Long bookId, Long userId);
    void deleteBook(Long bookId, Long userId);
    BookDetailDTO updateBook(Long bookId, BookPublishRequestDTO request, Long userId);
    void syncRatingStatsFromBook(Book book);
    PaginatedResponse<BookSummaryDTO> getBooksByFormatAvailability(String formatType, int page, int size, Long userId);
    PaginatedResponse<BookSummaryDTO> searchBooksByMultipleFilters(BookFilterDTO filter, int page, int size, Long userId);
}

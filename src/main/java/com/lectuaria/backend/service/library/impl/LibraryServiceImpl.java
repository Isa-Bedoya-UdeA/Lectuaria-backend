package com.lectuaria.backend.service.library.impl;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.library.LibrarySummaryDTO;
import com.lectuaria.backend.dto.statistics.GenreCountDTO;
import com.lectuaria.backend.dto.statistics.LibraryStatisticsDTO;
import com.lectuaria.backend.dto.statistics.PopularLibraryBookDTO;
import com.lectuaria.backend.exception.ResourceNotFoundException;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.LibraryBook;
import com.lectuaria.backend.model.library.Librarian;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.repository.library.LibraryBookRepository;
import com.lectuaria.backend.repository.library.LibraryRepository;
import com.lectuaria.backend.service.library.ILibraryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryServiceImpl implements ILibraryService {

    private final LibraryRepository libraryRepository;
    private final LibrarianRepository librarianRepository;
    private final LibraryBookRepository libraryBookRepository;

    public LibraryServiceImpl(LibraryRepository libraryRepository, LibrarianRepository librarianRepository,
            LibraryBookRepository libraryBookRepository) {
        this.libraryRepository = libraryRepository;
        this.librarianRepository = librarianRepository;
        this.libraryBookRepository = libraryBookRepository;
    }

    public List<LibrarySummaryDTO> getAllLibraries() {
        return libraryRepository.findAll().stream()
                .map(lib -> new LibrarySummaryDTO(
                        lib.getId(),
                        lib.getName(),
                        lib.getDescription(),
                        lib.getAddress(),
                        lib.getContactEmail(),
                        lib.getContactPhone(),
                        lib.getOpeningHours(),
                        null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LibraryStatisticsDTO getMyLibraryStatistics(User user) {
        if (user.getRole() != UserRole.LIBRARIAN && user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Solo los bibliotecarios pueden consultar estadísticas del catálogo");
        }

        Librarian librarian = librarianRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bibliotecario no encontrado"));
        Long libraryId = librarian.getLibrary().getId();
        Instant monthStart = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant updatedAt = Instant.now();

        List<PopularLibraryBookDTO> popularBooks = libraryBookRepository
                .findMostPopularByLibraryId(libraryId, PageRequest.of(0, 10))
                .stream()
                .map(libraryBook -> {
                    Book book = libraryBook.getBook();
                    long reviewCount = libraryBookRepository.countPublishedReviewsByBookId(book.getId());
                    int ratingsCount = book.getRatingsCount() != null ? book.getRatingsCount() : 0;
                    return new PopularLibraryBookDTO(toSummaryDTO(book, libraryId, libraryBook.getUserAdded() != null
                            ? libraryBook.getUserAdded().getId() : null), reviewCount + ratingsCount, reviewCount, ratingsCount);
                })
                .toList();

        BigDecimal averageRating = libraryBookRepository.calculateAverageRatingByLibraryId(libraryId)
                .setScale(2, RoundingMode.HALF_UP);

        return new LibraryStatisticsDTO(
                libraryBookRepository.countByLibraryId(libraryId),
                libraryBookRepository.countByLibraryIdAndAddedAtGreaterThanEqual(libraryId, monthStart),
                libraryBookRepository.findTopGenresByLibraryId(libraryId, PageRequest.of(0, 5)).stream()
                        .map(row -> new GenreCountDTO(((Number) row[0]).longValue(), (String) row[1], ((Number) row[2]).longValue()))
                        .toList(),
                libraryBookRepository.countPublishedReviewsByLibraryId(libraryId),
                averageRating,
                popularBooks,
                updatedAt,
                updatedAt.plusSeconds(86_400));
    }

    private BookSummaryDTO toSummaryDTO(Book book, Long libraryId, Long userAddedId) {
        List<String> authors = book.getAuthors() != null
                ? book.getAuthors().stream().map(Author::getName).collect(Collectors.toList())
                : List.of();
        List<GenreDTO> genres = book.getGenres() != null
                ? book.getGenres().stream()
                        .map(g -> new GenreDTO(g.getId(), g.getName(), g.getDescription()))
                        .collect(Collectors.toList())
                : List.of();
        return new BookSummaryDTO(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                authors,
                genres,
                book.getAverageRating() != null ? book.getAverageRating() : BigDecimal.ZERO,
                book.getRatingsCount() != null ? book.getRatingsCount() : 0,
                book.getCoverUrl(),
                libraryId,
                userAddedId,
                book.getCreatedBy() != null ? book.getCreatedBy().getId() : null,
                book.getCreatedAt());
    }
}

package com.lectuaria.backend.service.library.impl;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.library.LibrarySummaryDTO;
import com.lectuaria.backend.dto.statistics.GenreCountDTO;
import com.lectuaria.backend.dto.statistics.LibraryStatisticsDTO;
import com.lectuaria.backend.dto.statistics.PopularLibraryBookDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.Genre;
import com.lectuaria.backend.model.book.LibraryBook;
import com.lectuaria.backend.model.library.Library;
import com.lectuaria.backend.model.library.Librarian;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.repository.library.LibraryBookRepository;
import com.lectuaria.backend.repository.library.LibraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceImplTest {

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private LibrarianRepository librarianRepository;

    @Mock
    private LibraryBookRepository libraryBookRepository;

    @InjectMocks
    private LibraryServiceImpl libraryService;

    private User librarianUser;
    private User readerUser;
    private Library library;
    private Librarian librarian;
    private Book book;

    @BeforeEach
    void setUp() {
        librarianUser = new User("Librarian Name", "librarian@test.com", "hash", UserRole.LIBRARIAN, "lib_test", null, null);
        setId(librarianUser, 1L);

        readerUser = new User("Reader Name", "reader@test.com", "hash", UserRole.READER, "reader_test", null, null);
        setId(readerUser, 2L);

        library = new Library("Central Library", "A central public library", "123 Main St", "central@test.com", "555-1234", "9-5", 1L);
        setId(library, 10L);

        librarian = new Librarian(librarianUser, library, "central@test.com");
        setId(librarian, 100L);

        Author author = new Author();
        author.setName("Gabriel García Márquez");
        setId(author, 200L);

        Genre genre = new Genre();
        genre.setName("Realismo mágico");
        setId(genre, 300L);

        book = new Book();
        book.setIsbn(9780307474278L);
        book.setTitle("Cien años de soledad");
        book.setAuthors(List.of(author));
        book.setGenres(List.of(genre));
        book.setAverageRating(new BigDecimal("4.5"));
        book.setRatingsCount(1200);
        setId(book, 50L);
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getAllLibraries_returnsAllLibrariesAsDTO() {
        when(libraryRepository.findAll()).thenReturn(List.of(library));

        List<LibrarySummaryDTO> result = libraryService.getAllLibraries();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Central Library");
        assertThat(result.get(0).getEmail()).isEqualTo("central@test.com");
        verify(libraryRepository).findAll();
    }

    @Test
    void getAllLibraries_returnsEmptyListWhenNoLibraries() {
        when(libraryRepository.findAll()).thenReturn(Collections.emptyList());

        List<LibrarySummaryDTO> result = libraryService.getAllLibraries();

        assertThat(result).isEmpty();
    }

    @Test
    void getMyLibraryStatistics_throwsForReaderRole() {
        assertThatThrownBy(() -> libraryService.getMyLibraryStatistics(readerUser))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Solo los bibliotecarios pueden consultar estadísticas del catálogo");
    }

    @Test
    void getMyLibraryStatistics_throwsForAdminRole() {
        User adminUser = new User("Admin", "admin@test.com", "hash", UserRole.ADMIN, "admin_test", null, null);
        setId(adminUser, 3L);

        // Admin can access library statistics (same as librarian)
        when(librarianRepository.findByUserId(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libraryService.getMyLibraryStatistics(adminUser))
                .isInstanceOf(com.lectuaria.backend.exception.ResourceNotFoundException.class)
                .hasMessage("Bibliotecario no encontrado");
    }

    @Test
    void getMyLibraryStatistics_throwsWhenLibrarianNotFound() {
        when(librarianRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libraryService.getMyLibraryStatistics(librarianUser))
                .isInstanceOf(com.lectuaria.backend.exception.ResourceNotFoundException.class)
                .hasMessage("Bibliotecario no encontrado");
    }

    @Test
    void getMyLibraryStatistics_returnsStatisticsForLibrarian() {
        when(librarianRepository.findByUserId(1L)).thenReturn(Optional.of(librarian));
        when(libraryBookRepository.countByLibraryId(10L)).thenReturn(42L);
        when(libraryBookRepository.countByLibraryIdAndAddedAtGreaterThanEqual(eq(10L), any(Instant.class)))
                .thenReturn(5L);
        when(libraryBookRepository.findTopGenresByLibraryId(eq(10L), any(PageRequest.class)))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{300L, "Realismo mágico", 10L}));
        when(libraryBookRepository.countPublishedReviewsByLibraryId(10L)).thenReturn(15L);
        when(libraryBookRepository.calculateAverageRatingByLibraryId(10L))
                .thenReturn(new BigDecimal("4.25"));

        LibraryBook lb = new LibraryBook(library, book, 3, true, 1L, librarianUser);
        setId(lb, 1L);
        when(libraryBookRepository.findMostPopularByLibraryId(eq(10L), any(PageRequest.class)))
                .thenReturn(java.util.List.<LibraryBook>of(lb));
        when(libraryBookRepository.countPublishedReviewsByBookId(50L)).thenReturn(8L);

        LibraryStatisticsDTO result = libraryService.getMyLibraryStatistics(librarianUser);

        assertThat(result.getTotalBooks()).isEqualTo(42L);
        assertThat(result.getBooksAddedThisMonth()).isEqualTo(5L);
        assertThat(result.getReviewsOnOwnBooks()).isEqualTo(15L);
        assertThat(result.getAverageRatingOfOwnBooks()).isEqualTo(new BigDecimal("4.25"));
        assertThat(result.getMostPopularBooks()).hasSize(1);
        assertThat(result.getMostRepresentedGenres()).hasSize(1);
        assertThat(result.getMostRepresentedGenres().get(0).getGenreName()).isEqualTo("Realismo mágico");
    }

    @Test
    void getAllLibraries_mapsAllFieldsCorrectly() {
        Library lib2 = new Library("Branch Library", "A branch", "456 Oak Ave", "branch@test.com", "555-5678", "10-6", 2L);
        setId(lib2, 20L);
        when(libraryRepository.findAll()).thenReturn(List.of(library, lib2));

        List<LibrarySummaryDTO> result = libraryService.getAllLibraries();

        LibrarySummaryDTO first = result.stream().filter(l -> l.getName().equals("Central Library")).findFirst().get();
        assertThat(first.getAddress()).isEqualTo("123 Main St");
        assertThat(first.getPhone()).isEqualTo("555-1234");
        assertThat(first.getOpeningHours()).isEqualTo("9-5");

        LibrarySummaryDTO second = result.stream().filter(l -> l.getName().equals("Branch Library")).findFirst().get();
        assertThat(second.getAddress()).isEqualTo("456 Oak Ave");
        assertThat(second.getPhone()).isEqualTo("555-5678");
    }
}
package com.lectuaria.backend.service.book.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lectuaria.backend.dto.book.BookCatalogItemDTO;
import com.lectuaria.backend.dto.book.BookDetailDTO;
import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.book.FeaturedSectionsDTO;
import com.lectuaria.backend.dto.common.PaginatedResponse;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.Genre;
import com.lectuaria.backend.model.book.LibraryBook;
import com.lectuaria.backend.model.book.Publisher;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.library.Librarian;
import com.lectuaria.backend.model.library.Library;
import com.lectuaria.backend.repository.book.AuthorRepository;
import com.lectuaria.backend.repository.book.BookEditHistoryRepository;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.book.GenreRepository;
import com.lectuaria.backend.repository.book.PlatformRepository;
import com.lectuaria.backend.repository.book.PublisherRepository;
import com.lectuaria.backend.repository.library.LibraryBookRepository;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.service.book.IBookRatingService;
import com.lectuaria.backend.service.storage.S3StorageService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock private BookRepository bookRepository;
    @Mock private BookEditHistoryRepository bookEditHistoryRepository;
    @Mock private PlatformRepository platformRepository;
    @Mock private LibraryBookRepository libraryBookRepository;
    @Mock private LibrarianRepository librarianRepository;
    @Mock private IBookRatingService bookRatingService;
    @Mock private AuthorRepository authorRepository;
    @Mock private GenreRepository genreRepository;
    @Mock private PublisherRepository publisherRepository;
    @Mock private UserRepository userRepository;
    @Mock private EntityManager entityManager;
    @Mock private UserListBookRepository listBookRepository;
    @Mock private S3StorageService s3StorageService;
    @Mock private ObjectMapper objectMapper;

    private BookServiceImpl bookService;

    private void setId(Object entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }

    private void setField(Object entity, String fieldName, Object value) throws Exception {
        Field f = entity.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(entity, value);
    }

    @BeforeEach
    void setUp() {
        bookService = new BookServiceImpl(
                bookRepository, bookEditHistoryRepository, platformRepository,
                authorRepository, genreRepository, publisherRepository,
                libraryBookRepository, librarianRepository, bookRatingService,
                userRepository, entityManager, listBookRepository,
                s3StorageService, objectMapper);
    }

    // ===== SEARCH BOOKS TESTS =====

    @Nested
    @DisplayName("searchBooks()")
    class SearchBooksTests {

        @Test
        @DisplayName("returns paginated results with keyword search")
        void searchBooks_returnsPaginatedResults() throws Exception {
            Book book = createTestBook(1L, "Cien Años de Soledad", 123456L);

            Page<Book> page = new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1);
            when(bookRepository.searchBooks(eq("soledad"), any(Pageable.class))).thenReturn(page);

            PaginatedResponse<BookSummaryDTO> result = bookService.searchBooks("soledad", 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("Cien Años de Soledad", result.getContent().get(0).getTitle());
        }

        @Test
        @DisplayName("returns empty results when no match")
        void searchBooks_returnsEmptyWhenNoMatch() {
            Page<Book> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(bookRepository.searchBooks(eq("nonexistent"), any(Pageable.class))).thenReturn(emptyPage);

            PaginatedResponse<BookSummaryDTO> result = bookService.searchBooks("nonexistent", 0, 10);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
        }
    }

    // ===== GET BOOK BY ID TESTS =====

    @Nested
    @DisplayName("getBookById()")
    class GetBookByIdTests {

        @Test
        @DisplayName("returns book detail when found")
        void getBookById_returnsBookDetail() throws Exception {
            Book book = createTestBook(1L, "Test Book", 123456L);

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            BookDetailDTO result = bookService.getBookById(1L);

            assertNotNull(result);
            assertEquals("Test Book", result.getTitle());
            assertEquals(123456L, result.getIsbn());
        }

        @Test
        @DisplayName("throws RuntimeException when book not found")
        void getBookById_throwsWhenNotFound() {
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class, () -> bookService.getBookById(999L));
            assertTrue(ex.getMessage().contains("no encontrado"));
        }
    }

    // ===== GET BOOK BY ISBN TESTS =====

    @Nested
    @DisplayName("getBookByIsbn()")
    class GetBookByIsbnTests {

        @Test
        @DisplayName("returns book detail when found by ISBN")
        void getBookByIsbn_returnsBookDetail() throws Exception {
            Book book = createTestBook(1L, "ISBN Book", 9781234567890L);

            when(bookRepository.findByIsbn(9781234567890L)).thenReturn(Optional.of(book));

            BookDetailDTO result = bookService.getBookByIsbn(9781234567890L);

            assertNotNull(result);
            assertEquals("ISBN Book", result.getTitle());
            assertEquals(9781234567890L, result.getIsbn());
        }

        @Test
        @DisplayName("throws RuntimeException when book not found by ISBN")
        void getBookByIsbn_throwsWhenNotFound() {
            when(bookRepository.findByIsbn(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class, () -> bookService.getBookByIsbn(999L));
            assertTrue(ex.getMessage().contains("no encontrado"));
        }
    }

    // ===== GET SIMILAR BOOKS TESTS =====

    @Nested
    @DisplayName("getSimilarBooks()")
    class GetSimilarBooksTests {

        @Test
        @DisplayName("returns books by same author")
        void getSimilarBooks_returnsBooksByAuthor() throws Exception {
            Author author = new Author();
            setId(author, 10L);
            author.setName("Gabriel García Márquez");

            Genre genre = new Genre();
            setId(genre, 5L);
            genre.setName("Ficción");

            Book mainBook = createTestBook(1L, "Main Book", 123L);
            mainBook.setAuthors(List.of(author));
            mainBook.setGenres(List.of(genre));

            Book similar1 = createTestBook(2L, "Similar Book 1", 456L);
            similar1.setAuthors(List.of(author));
            similar1.setGenres(List.of(genre));
            Book similar2 = createTestBook(3L, "Similar Book 2", 789L);
            similar2.setAuthors(List.of(author));
            similar2.setGenres(List.of(genre));

            when(bookRepository.findById(1L)).thenReturn(Optional.of(mainBook));
            when(bookRepository.findSimilarByAuthorIds(eq(1L), anyList(), any(Pageable.class)))
                    .thenReturn(List.of(similar1, similar2));
            // Stub fallback for when genre branch runs
            lenient().when(bookRepository.findSimilarByGenreIds(eq(1L), anyList(), any(Pageable.class)))
                    .thenReturn(List.of());
            lenient().when(bookRepository.findQualifiedTopRated(any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            List<BookSummaryDTO> result = bookService.getSimilarBooks(1L);

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("throws RuntimeException when book not found")
        void getSimilarBooks_throwsWhenNotFound() {
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class, () -> bookService.getSimilarBooks(999L));
            assertTrue(ex.getMessage().contains("no encontrado"));
        }

        @Test
        @DisplayName("returns limited to 6 results")
        void getSimilarBooks_limitsTo6() throws Exception {
            Book mainBook = createTestBook(1L, "Main Book", 123L);
            mainBook.setAuthors(List.of());
            mainBook.setGenres(List.of());

            // Fallback returns 8 books (enough for 6)
            List<Book> fallbackBooks = new ArrayList<>();
            for (int i = 2; i <= 9; i++) {
                Book b = createTestBook((long) i, "Book " + i, (long) (100 + i));
                fallbackBooks.add(b);
            }

            when(bookRepository.findById(1L)).thenReturn(Optional.of(mainBook));
            // Both author and genre branches are skipped (no authors, no genres)
            lenient().when(bookRepository.findSimilarByAuthorIds(eq(1L), anyList(), any(Pageable.class)))
                    .thenReturn(List.of());
            lenient().when(bookRepository.findSimilarByGenreIds(eq(1L), anyList(), any(Pageable.class)))
                    .thenReturn(List.of());
            // Fallback branch returns 8 books, code takes first 6
            when(bookRepository.findQualifiedTopRated(any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(fallbackBooks));

            List<BookSummaryDTO> result = bookService.getSimilarBooks(1L);

            assertEquals(6, result.size());
        }
    }

    // ===== GET FEATURED SECTIONS TESTS =====

    @Nested
    @DisplayName("getFeaturedSections()")
    class GetFeaturedSectionsTests {

        @Test
        @DisplayName("returns featured sections with most read and top rated")
        void getFeaturedSections_returnsAllSections() throws Exception {
            Book mostReadBook = createTestBook(1L, "Most Read Book", 111L);
            Book topRatedBook = createTestBook(2L, "Top Rated Book", 222L);

            when(listBookRepository.findMostAddedToListSince(anyString(), any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of(mostReadBook));
            when(bookRepository.findQualifiedTopRated(any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(topRatedBook)));

            FeaturedSectionsDTO result = bookService.getFeaturedSections();

            assertNotNull(result);
            assertNotNull(result.getMostReadThisMonth());
            assertNotNull(result.getTopRated());
            assertNotNull(result.getNextUpdateAt());
            assertEquals(1, result.getMostReadThisMonth().size());
            assertEquals(1, result.getTopRated().size());
        }

        @Test
        @DisplayName("returns empty sections when no data")
        void getFeaturedSections_returnsEmptySections() {
            when(listBookRepository.findMostAddedToListSince(anyString(), any(Instant.class), any(Pageable.class)))
                    .thenReturn(List.of());
            when(bookRepository.findQualifiedTopRated(any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            FeaturedSectionsDTO result = bookService.getFeaturedSections();

            assertNotNull(result);
            assertTrue(result.getMostReadThisMonth().isEmpty());
            assertTrue(result.getTopRated().isEmpty());
        }
    }

    // ===== REMOVE BOOK FROM LIBRARY TESTS =====

    @Nested
    @DisplayName("removeBookFromLibrary()")
    class RemoveBookFromLibraryTests {

        @Test
        @DisplayName("removes book from library successfully")
        void removeBookFromLibrary_success() throws Exception {
            User librarianUser = new User("Lib", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);
            setId(librarianUser, 1L);

            Library library = new Library("Test Library", "Desc", "Addr", "lib@test.com", "111", "9-5", 1L);
            setId(library, 5L);

            Librarian librarian = new Librarian(librarianUser, library, "lib@test.com");
            setId(librarian, 1L);

            Book book = createTestBook(10L, "Library Book", 123L);
            setField(book, "createdBy", librarianUser);

            LibraryBook libraryBook = new LibraryBook();
            setId(libraryBook, 100L);
            setField(libraryBook, "book", book);
            setField(libraryBook, "library", library);

            when(userRepository.findById(1L)).thenReturn(Optional.of(librarianUser));
            when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
            when(libraryBookRepository.findByLibraryIdAndBookId(5L, 10L)).thenReturn(Optional.of(libraryBook));
            when(libraryBookRepository.countByBookId(10L)).thenReturn(0L);
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> bookService.removeBookFromLibrary(10L, 1L));

            verify(libraryBookRepository).delete(libraryBook);
            verify(entityManager, atLeastOnce()).flush();
        }

        @Test
        @DisplayName("throws UnauthorizedException when user is not librarian")
        void removeBookFromLibrary_throwsWhenNotLibrarian() throws Exception {
            User readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
            setId(readerUser, 1L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(readerUser));

            assertThrows(com.lectuaria.backend.exception.UnauthorizedException.class,
                    () -> bookService.removeBookFromLibrary(10L, 1L));
        }

        @Test
        @DisplayName("throws RuntimeException when book not in library")
        void removeBookFromLibrary_throwsWhenNotInLibrary() throws Exception {
            User librarianUser = new User("Lib", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);
            setId(librarianUser, 1L);

            Library library = new Library("Test Library", "Desc", "Addr", "lib@test.com", "111", "9-5", 1L);
            setId(library, 5L);

            Librarian librarian = new Librarian(librarianUser, library, "lib@test.com");
            setId(librarian, 1L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(librarianUser));
            when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
            when(libraryBookRepository.findByLibraryIdAndBookId(5L, 999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> bookService.removeBookFromLibrary(999L, 1L));
            assertTrue(ex.getMessage().contains("no está en tu biblioteca"));
        }

        @Test
        @DisplayName("deletes book from DB when no libraries have it")
        void removeBookFromLibrary_deletesBookWhenNoLibraries() throws Exception {
            User librarianUser = new User("Lib", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);
            setId(librarianUser, 1L);

            Library library = new Library("Test Library", "Desc", "Addr", "lib@test.com", "111", "9-5", 1L);
            setId(library, 5L);

            Librarian librarian = new Librarian(librarianUser, library, "lib@test.com");
            setId(librarian, 1L);

            Book book = createTestBook(10L, "Orphan Book", 123L);
            book.setCoverUrl("https://s3.aws.com/cover.jpg");

            LibraryBook libraryBook = new LibraryBook();
            setId(libraryBook, 100L);
            setField(libraryBook, "book", book);
            setField(libraryBook, "library", library);

            when(userRepository.findById(1L)).thenReturn(Optional.of(librarianUser));
            when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
            when(libraryBookRepository.findByLibraryIdAndBookId(5L, 10L)).thenReturn(Optional.of(libraryBook));
            when(libraryBookRepository.countByBookId(10L)).thenReturn(0L);

            bookService.removeBookFromLibrary(10L, 1L);

            verify(s3StorageService).deleteCover("https://s3.aws.com/cover.jpg");
            verify(bookRepository).deleteById(10L);
        }
    }

    // ===== DELETE BOOK TESTS =====

    @Nested
    @DisplayName("deleteBook()")
    class DeleteBookTests {

        @Test
        @DisplayName("deletes book when user is admin")
        void deleteBook_asAdmin() throws Exception {
            User admin = new User("Admin", "admin@test.com", "hash", UserRole.ADMIN, "admin", null, null);
            setId(admin, 1L);

            Book book = createTestBook(10L, "Book to Delete", 123L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            assertDoesNotThrow(() -> bookService.deleteBook(10L, 1L));

            verify(bookRepository).delete(book);
        }

        @Test
        @DisplayName("throws UnauthorizedException when user is not admin")
        void deleteBook_throwsWhenNotAdmin() throws Exception {
            User librarian = new User("Lib", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);
            setId(librarian, 1L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(librarian));

            assertThrows(com.lectuaria.backend.exception.UnauthorizedException.class,
                    () -> bookService.deleteBook(10L, 1L));
        }

        @Test
        @DisplayName("throws RuntimeException when book not found")
        void deleteBook_throwsWhenBookNotFound() throws Exception {
            User admin = new User("Admin", "admin@test.com", "hash", UserRole.ADMIN, "admin", null, null);
            setId(admin, 1L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> bookService.deleteBook(999L, 1L));
            assertEquals("Libro no encontrado", ex.getMessage());
        }
    }

    // ===== SYNC RATING STATS TESTS =====

    @Nested
    @DisplayName("syncRatingStatsFromBook()")
    class SyncRatingStatsTests {

        @Test
        @DisplayName("calls bookRatingService to sync stats")
        void syncRatingStatsFromBook_callsService() throws Exception {
            Book book = createTestBook(1L, "Test Book", 123L);

            bookService.syncRatingStatsFromBook(book);

            verify(bookRatingService).syncBookRatingStatsFromBook(book);
        }
    }

    // ===== HELPER METHODS =====

    private Book createTestBook(Long id, String title, Long isbn) throws Exception {
        Book book = new Book();
        setId(book, id);
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setAverageRating(BigDecimal.valueOf(4.0));
        book.setRatingsCount(10);
        book.setPages(200);
        book.setDescription("Test description");
        book.setCoverUrl("http://example.com/cover.jpg");
        book.setPublicationDate(LocalDate.of(2020, 1, 1));
        return book;
    }
}
package com.lectuaria.backend.service.book.impl;

import com.lectuaria.backend.dto.book.*;
import com.lectuaria.backend.dto.book.externalApi.ExternalBookMetadataDTO;
import com.lectuaria.backend.exception.BookAlreadyExistsInLibraryException;
import com.lectuaria.backend.exception.ForbiddenException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.Genre;
import com.lectuaria.backend.model.book.LibraryBook;
import com.lectuaria.backend.model.book.Publisher;
import com.lectuaria.backend.model.library.Librarian;
import com.lectuaria.backend.model.library.Library;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.book.AuthorRepository;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.book.GenreRepository;
import com.lectuaria.backend.repository.book.PlatformRepository;
import com.lectuaria.backend.repository.book.PublisherRepository;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.repository.library.LibraryBookRepository;
import com.lectuaria.backend.service.book.externalApi.IExternalBookMetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookPublishServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private PlatformRepository platformRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private PublisherRepository publisherRepository;

    @Mock
    private LibrarianRepository librarianRepository;

    @Mock
    private LibraryBookRepository libraryBookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IExternalBookMetadataService externalBookMetadataService;

    @InjectMocks
    private BookPublishServiceImpl service;

    private User librarianUser;
    private User readerUser;
    private Librarian librarian;
    private Library library;
    private Book existingBook;
    private Genre validGenre;
    private Author existingAuthor;
    private Publisher existingPublisher;

    @BeforeEach
    void setUp() {
        librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "libuser", null, null);
        setId(librarianUser, 1L);

        readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 2L);

        library = new Library("Test Library", "Desc", "Address", "lib@lib.com", "555", "9-5", 1L);
        setId(library, 10L);

        librarian = new Librarian(librarianUser, library, "lib@lib.com");
        setId(librarian, 100L);

        validGenre = new Genre();
        validGenre.setName("Fiction");
        setId(validGenre, 200L);

        existingAuthor = new Author();
        existingAuthor.setName("Existing Author");
        setId(existingAuthor, 300L);

        existingPublisher = new Publisher();
        existingPublisher.setName("Existing Publisher");
        setId(existingPublisher, 400L);

        existingBook = new Book();
        existingBook.setTitle("Existing Book");
        existingBook.setIsbn(9781234567890L);
        existingBook.setAuthors(List.of(existingAuthor));
        existingBook.setGenres(List.of(validGenre));
        existingBook.setPublishers(List.of(existingPublisher));
        setId(existingBook, 50L);
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

    private BookPublishRequestDTO createValidRequest() {
        BookPublishRequestDTO request = new BookPublishRequestDTO();
        request.setIsbn(9781234567890L);
        request.setTitle("New Book");
        request.setDescription("Description");
        request.setAuthors(List.of("New Author"));
        request.setGenres(List.of("Fiction"));
        request.setPublishers(List.of("New Publisher"));
        request.setPages(300);
        request.setPublicationDate(LocalDate.of(2020, 1, 1));
        request.setCoverUrl("http://example.com/cover.jpg");
        AvailabilityDTO availability = new AvailabilityDTO();
        availability.setPhysical(true);
        availability.setPhysicalCopies(5);
        availability.setDigital(false);
        request.setAvailability(availability);
        return request;
    }

    // ========== publishBook ==========

    @Nested
    class PublishBookTests {

        @Test
        void publishBook_validRequest_createsNewBook() {
            BookPublishRequestDTO request = createValidRequest();
            request.setIsbn(9781111111111L); // New ISBN

            when(userRepository.findById(1L)).thenReturn(Optional.of(librarianUser));
            when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
            when(bookRepository.findByIsbn(9781111111111L)).thenReturn(Optional.empty());
            when(bookRepository.save(any())).thenAnswer(inv -> {
                Book b = inv.getArgument(0);
                setId(b, 999L);
                return b;
            });
            when(genreRepository.findByName("Fiction")).thenReturn(Optional.of(validGenre));
            when(authorRepository.findByName("New Author")).thenReturn(Optional.empty());
            when(authorRepository.save(any())).thenAnswer(inv -> {
                Author a = inv.getArgument(0);
                setId(a, 888L);
                return a;
            });
            when(publisherRepository.findByName("New Publisher")).thenReturn(Optional.empty());
            when(publisherRepository.save(any())).thenAnswer(inv -> {
                Publisher p = inv.getArgument(0);
                setId(p, 777L);
                return p;
            });
            when(libraryBookRepository.existsByLibraryIdAndBookId(10L, 999L)).thenReturn(false);
            when(libraryBookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BookPublishResponseDTO result = service.publishBook(request, 1L);

            assertThat(result).isNotNull();
            assertThat(result.isNewBook()).isTrue();
            assertThat(result.getTitle()).isEqualTo("New Book");
            assertThat(result.getMessage()).contains("creado");
            verify(bookRepository).save(any());
            verify(libraryBookRepository).save(any());
        }

        @Test
        void publishBook_existingBook_usesExisting() {
            BookPublishRequestDTO request = createValidRequest();

            when(userRepository.findById(1L)).thenReturn(Optional.of(librarianUser));
            when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
            when(bookRepository.findByIsbn(9781234567890L)).thenReturn(Optional.of(existingBook));
            when(libraryBookRepository.existsByLibraryIdAndBookId(10L, 50L)).thenReturn(false);
            when(libraryBookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BookPublishResponseDTO result = service.publishBook(request, 1L);

            assertThat(result).isNotNull();
            assertThat(result.isNewBook()).isFalse();
            assertThat(result.getMessage()).contains("ya existía");
            verify(bookRepository, never()).save(any());
        }

        @Test
        void publishBook_alreadyInLibrary_throws() {
            BookPublishRequestDTO request = createValidRequest();

            when(userRepository.findById(1L)).thenReturn(Optional.of(librarianUser));
            when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
            when(bookRepository.findByIsbn(9781234567890L)).thenReturn(Optional.of(existingBook));
            when(libraryBookRepository.existsByLibraryIdAndBookId(10L, 50L)).thenReturn(true);

            assertThatThrownBy(() -> service.publishBook(request, 1L))
                    .isInstanceOf(BookAlreadyExistsInLibraryException.class);
        }

        @Test
        void publishBook_readerRole_throwsForbidden() {
            BookPublishRequestDTO request = createValidRequest();
            when(userRepository.findById(2L)).thenReturn(Optional.of(readerUser));

            assertThatThrownBy(() -> service.publishBook(request, 2L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Solo los bibliotecarios");
        }

        @Test
        void publishBook_userNotLibrarian_throws() {
            BookPublishRequestDTO request = createValidRequest();
            when(userRepository.findById(1L)).thenReturn(Optional.of(librarianUser));
            when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.publishBook(request, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no tiene una biblioteca");
        }

        @Test
        void publishBook_invalidGenre_throws() {
            BookPublishRequestDTO request = createValidRequest();

            when(userRepository.findById(1L)).thenReturn(Optional.of(librarianUser));
            when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
            when(bookRepository.findByIsbn(9781111111111L)).thenReturn(Optional.empty());
            when(genreRepository.findByName("Fiction")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.publishBook(request, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no existe en la plataforma");
        }

        @Test
        void publishBook_usesExistingAuthor() {
            BookPublishRequestDTO request = createValidRequest();
            request.setIsbn(9781111111111L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(librarianUser));
            when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
            when(bookRepository.findByIsbn(9781111111111L)).thenReturn(Optional.empty());
            when(bookRepository.save(any())).thenAnswer(inv -> {
                Book b = inv.getArgument(0);
                setId(b, 999L);
                return b;
            });
            when(genreRepository.findByName("Fiction")).thenReturn(Optional.of(validGenre));
            when(authorRepository.findByName("New Author")).thenReturn(Optional.of(existingAuthor));
            when(publisherRepository.findByName("New Publisher")).thenReturn(Optional.empty());
            when(publisherRepository.save(any())).thenAnswer(inv -> {
                Publisher p = inv.getArgument(0);
                setId(p, 777L);
                return p;
            });
            when(libraryBookRepository.existsByLibraryIdAndBookId(10L, 999L)).thenReturn(false);
            when(libraryBookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.publishBook(request, 1L);

            verify(authorRepository, never()).save(any());
        }
    }

    // ========== prefillFromOpenLibrary ==========

    @Nested
    class PrefillFromOpenLibraryTests {

        @Test
        void prefillFromOpenLibrary_bookExistsInCatalog_returnsFromLocal() {
            when(bookRepository.findByIsbn(9781234567890L)).thenReturn(Optional.of(existingBook));
            when(librarianRepository.findByUserId(1L)).thenReturn(Optional.of(librarian));
            when(libraryBookRepository.existsByLibraryIdAndBookId(10L, 50L)).thenReturn(false);

            BookPublishRequestDTO result = service.prefillFromOpenLibrary(9781234567890L, 1L);

            assertThat(result.getBookExistsInCatalog()).isTrue();
            assertThat(result.getBookExistsInUserLibrary()).isFalse();
            assertThat(result.getTitle()).isEqualTo("Existing Book");
            assertThat(result.getAuthors()).contains("Existing Author");
            verify(externalBookMetadataService, never()).fetchBookMetadata(any());
        }

        @Test
        void prefillFromOpenLibrary_bookAlreadyInUserLibrary() {
            when(bookRepository.findByIsbn(9781234567890L)).thenReturn(Optional.of(existingBook));
            when(librarianRepository.findByUserId(1L)).thenReturn(Optional.of(librarian));
            when(libraryBookRepository.existsByLibraryIdAndBookId(10L, 50L)).thenReturn(true);

            BookPublishRequestDTO result = service.prefillFromOpenLibrary(9781234567890L, 1L);

            assertThat(result.getBookExistsInUserLibrary()).isTrue();
        }

        @Test
        void prefillFromOpenLibrary_notFound_locally_fetchesExternal() {
            ExternalBookMetadataDTO external = new ExternalBookMetadataDTO();
            external.setTitle("External Book");
            external.setDescription("External Desc");
            external.setAuthors(List.of("External Author"));
            external.setPublisher("External Publisher");
            external.setPageCount(200);
            external.setPublishedDate("2021-01-01");
            external.setCoverUrl("http://external.com/cover.jpg");

            when(bookRepository.findByIsbn(9781234567890L)).thenReturn(Optional.empty());
            when(librarianRepository.findByUserId(1L)).thenReturn(Optional.of(librarian));
            when(externalBookMetadataService.fetchBookMetadata(9781234567890L)).thenReturn(external);

            BookPublishRequestDTO result = service.prefillFromOpenLibrary(9781234567890L, 1L);

            assertThat(result.getBookExistsInCatalog()).isFalse();
            assertThat(result.getTitle()).isEqualTo("External Book");
            assertThat(result.getAuthors()).contains("External Author");
            assertThat(result.getPublishers()).contains("External Publisher");
            assertThat(result.getPages()).isEqualTo(200);
        }

        @Test
        void prefillFromOpenLibrary_notFoundAnywhere_returnsEmpty() {
            when(bookRepository.findByIsbn(9781234567890L)).thenReturn(Optional.empty());
            when(librarianRepository.findByUserId(1L)).thenReturn(Optional.of(librarian));
            when(externalBookMetadataService.fetchBookMetadata(9781234567890L)).thenReturn(null);

            BookPublishRequestDTO result = service.prefillFromOpenLibrary(9781234567890L, 1L);

            assertThat(result.getBookExistsInCatalog()).isFalse();
            assertThat(result.getIsbn()).isEqualTo(9781234567890L);
            assertThat(result.getTitle()).isNull();
        }

        @Test
        void prefillFromOpenLibrary_librarianNotFound_throws() {
            when(librarianRepository.findByUserId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.prefillFromOpenLibrary(9781234567890L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Bibliotecario no encontrado");
        }

        @Test
        void prefillFromOpenLibrary_externalNullPublisher_handlesGracefully() {
            ExternalBookMetadataDTO external = new ExternalBookMetadataDTO();
            external.setTitle("Book");
            external.setPublisher(null);
            external.setAuthors(List.of());

            when(bookRepository.findByIsbn(9781234567890L)).thenReturn(Optional.empty());
            when(librarianRepository.findByUserId(1L)).thenReturn(Optional.of(librarian));
            when(externalBookMetadataService.fetchBookMetadata(9781234567890L)).thenReturn(external);

            BookPublishRequestDTO result = service.prefillFromOpenLibrary(9781234567890L, 1L);

            assertThat(result.getPublishers()).isEmpty();
        }

        @Test
        void prefillFromOpenLibrary_externalInvalidDate_handlesGracefully() {
            ExternalBookMetadataDTO external = new ExternalBookMetadataDTO();
            external.setTitle("Book");
            external.setPublishedDate("not-a-date");

            when(bookRepository.findByIsbn(9781234567890L)).thenReturn(Optional.empty());
            when(librarianRepository.findByUserId(1L)).thenReturn(Optional.of(librarian));
            when(externalBookMetadataService.fetchBookMetadata(9781234567890L)).thenReturn(external);

            BookPublishRequestDTO result = service.prefillFromOpenLibrary(9781234567890L, 1L);

            assertThat(result.getPublicationDate()).isNull();
        }
    }
}
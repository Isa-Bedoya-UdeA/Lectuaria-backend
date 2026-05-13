package com.lectuaria.backend.service.book.impl;

import com.lectuaria.backend.dto.book.*;
import com.lectuaria.backend.service.book.IBookService;
import com.lectuaria.backend.service.book.IBookRatingService;
import com.lectuaria.backend.dto.common.PaginatedResponse;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.Genre;
import com.lectuaria.backend.model.book.LibraryBook;
import com.lectuaria.backend.model.book.Publisher;
import com.lectuaria.backend.repository.book.AuthorRepository;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.book.FormatRepository;
import com.lectuaria.backend.repository.book.GenreRepository;
import com.lectuaria.backend.repository.library.LibraryBookRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.repository.book.PublisherRepository;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.specification.BookSpecifications;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.util.ISBNValidator;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.library.Librarian;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookServiceImpl implements IBookService {

    private final BookRepository bookRepository;
    private final LibraryBookRepository libraryBookRepository;
    private final LibrarianRepository librarianRepository;
    private final IBookRatingService bookRatingService;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final PublisherRepository publisherRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final UserListBookRepository listBookRepository;

    public BookServiceImpl(BookRepository bookRepository,
            AuthorRepository authorRepository,
            GenreRepository genreRepository,
            PublisherRepository publisherRepository,
            FormatRepository formatRepository,
            LibraryBookRepository libraryBookRepository,
            LibrarianRepository librarianRepository,
            IBookRatingService bookRatingService,
            UserRepository userRepository,
            EntityManager entityManager,
            UserListBookRepository listBookRepository) {
        this.bookRepository = bookRepository;
        this.libraryBookRepository = libraryBookRepository;
        this.librarianRepository = librarianRepository;
        this.bookRatingService = bookRatingService;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.publisherRepository = publisherRepository;
        this.entityManager = entityManager;
        this.userRepository = userRepository;
        this.listBookRepository = listBookRepository;
    }

    public PaginatedResponse<BookSummaryDTO> searchBooks(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());
        Page<Book> bookPage = bookRepository.searchBooks(keyword, pageable);
        return toPaginatedResponseFromBooks(bookPage, this::toSummaryDTO);
    }

    public PaginatedResponse<BookSummaryDTO> getAllBooks(int page, int size, Float minRating, Integer startYear,
            Integer endYear, List<String> formatTypes, Long userId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());

        Specification<Book> spec = Specification.where(null);
        if (minRating != null && minRating > 0) {
            spec = spec.and(BookSpecifications.hasMinimumRating(minRating));
        }
        if (startYear != null) {
            spec = spec.and(BookSpecifications.hasMinPublicationYear(startYear));
        }
        if (endYear != null) {
            spec = spec.and(BookSpecifications.hasMaxPublicationYear(endYear));
        }
        if (formatTypes != null && !formatTypes.isEmpty()) {
            spec = spec.and(BookSpecifications.hasFormatTypes(formatTypes));
        }

        Page<Book> bookPage = bookRepository.findAll(spec, pageable);

        // Obtener la biblioteca del usuario si es bibliotecario
        final Long libraryId;
        if (userId != null) {
            libraryId = librarianRepository.findByUserId(userId)
                    .map(librarian -> librarian.getLibrary().getId())
                    .orElse(null);
        } else {
            libraryId = null;
        }

        return toPaginatedResponseFromBooks(bookPage, book -> {
            if (libraryId != null) {
                // Verificar si el usuario ha añadido este libro a su biblioteca
                Optional<LibraryBook> libraryBook = libraryBookRepository
                        .findByLibraryIdAndBookId(libraryId, book.getId());
                Long userAddedId = libraryBook.map(lb -> lb.getUserAdded().getId()).orElse(null);
                return toSummaryDTOWithLibraryInfo(book, libraryId, userAddedId);
            }
            return toSummaryDTO(book);
        });
    }

    public PaginatedResponse<BookSummaryDTO> getBooksByGenre(Long genreId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findByGenreId(genreId, pageable);
        return toPaginatedResponseFromBooks(bookPage, this::toSummaryDTO);
    }

    public PaginatedResponse<BookSummaryDTO> getBooksByLibrary(Long libraryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LibraryBook> libraryBookPage = libraryBookRepository.findByLibraryId(libraryId, pageable);
        return toPaginatedResponseFromLibraryBooks(libraryBookPage, lb -> {
            Long userAddedId = lb.getUserAdded() != null ? lb.getUserAdded().getId() : null;
            return toSummaryDTOWithLibraryInfo(lb.getBook(), libraryId, userAddedId);
        });
    }

    public PaginatedResponse<BookSummaryDTO> getBooksByGenres(List<Long> genreIds, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findByGenreIdsIn(genreIds, pageable);
        return toPaginatedResponseFromBooks(bookPage, this::toSummaryDTO);
    }

    public PaginatedResponse<BookSummaryDTO> getBooksByGenresWithLibraryInfo(List<Long> genreIds, int page, int size,
            Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findByGenreIdsIn(genreIds, pageable);

        // Obtener la biblioteca del usuario si es bibliotecario
        final Long libraryId;
        if (userId != null) {
            libraryId = librarianRepository.findByUserId(userId)
                    .map(librarian -> librarian.getLibrary().getId())
                    .orElse(null);
        } else {
            libraryId = null;
        }

        return toPaginatedResponseFromBooks(bookPage, book -> {
            if (libraryId != null) {
                // Verificar si el usuario ha añadido este libro a su biblioteca
                Optional<LibraryBook> libraryBook = libraryBookRepository
                        .findByLibraryIdAndBookId(libraryId, book.getId());
                Long userAddedId = libraryBook.map(lb -> lb.getUserAdded().getId()).orElse(null);
                return toSummaryDTOWithLibraryInfo(book, libraryId, userAddedId);
            }
            return toSummaryDTO(book);
        });
    }

    public PaginatedResponse<BookSummaryDTO> getBooksByAuthor(Long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findByAuthorId(authorId, pageable);
        return toPaginatedResponseFromBooks(bookPage, this::toSummaryDTO);
    }

    public PaginatedResponse<BookSummaryDTO> getMostPopular(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findMostPopular(pageable);
        return toPaginatedResponseFromBooks(bookPage, this::toSummaryDTO);
    }

    public PaginatedResponse<BookSummaryDTO> getTopRated(int page, int size, Long genreId, Integer year) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findQualifiedTopRated(genreId, year, pageable);
        return toPaginatedResponseFromBooks(bookPage, this::toSummaryDTO);
    }

    public PaginatedResponse<BookCatalogItemDTO> getNewCatalogBooks(int page, int size, Long genreId,
            String formatName) {
        Pageable pageable = PageRequest.of(page, size);
        Instant since = Instant.now().minus(java.time.Duration.ofDays(30));
        Page<Book> bookPage = bookRepository.findNewCatalogBooks(since, genreId, normalizeBlank(formatName), pageable);
        return toPaginatedResponseFromBooks(bookPage,
                book -> new BookCatalogItemDTO(toSummaryDTO(book), book.getCreatedAt()));
    }

    public FeaturedSectionsDTO getFeaturedSections() {
        Instant monthStart = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant weekStart = LocalDate.now(ZoneOffset.UTC)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay().toInstant(ZoneOffset.UTC);

        List<BookSummaryDTO> mostRead = listToSummary(
                listBookRepository.findMostAddedToListSince("Leídos", monthStart, PageRequest.of(0, 10)));

        List<BookSummaryDTO> topRated = listToSummary(
                bookRepository.findQualifiedTopRated(null, null, PageRequest.of(0, 10)).getContent());

        Instant nextUpdateAt = LocalDate.now(ZoneOffset.UTC)
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                .atStartOfDay().toInstant(ZoneOffset.UTC);
        return new FeaturedSectionsDTO(mostRead, topRated, nextUpdateAt);
    }

    public List<BookSummaryDTO> getSimilarBooks(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Libro no encontrado con ID: " + bookId));

        Map<Long, Book> similarBooks = new LinkedHashMap<>();
        List<Long> authorIds = book.getAuthors() != null
                ? book.getAuthors().stream().map(Author::getId).collect(Collectors.toList())
                : List.of();
        if (!authorIds.isEmpty()) {
            bookRepository.findSimilarByAuthorIds(bookId, authorIds, PageRequest.of(0, 6))
                    .forEach(candidate -> similarBooks.putIfAbsent(candidate.getId(), candidate));
        }

        List<Long> genreIds = book.getGenres() != null
                ? book.getGenres().stream().map(Genre::getId).collect(Collectors.toList())
                : List.of();
        if (similarBooks.size() < 6 && !genreIds.isEmpty()) {
            bookRepository.findSimilarByGenreIds(bookId, genreIds, PageRequest.of(0, 12))
                    .forEach(candidate -> similarBooks.putIfAbsent(candidate.getId(), candidate));
        }

        if (similarBooks.size() < 6) {
            bookRepository.findQualifiedTopRated(null, null, PageRequest.of(0, 12)).getContent().stream()
                    .filter(candidate -> !candidate.getId().equals(bookId))
                    .forEach(candidate -> similarBooks.putIfAbsent(candidate.getId(), candidate));
        }

        return similarBooks.values().stream()
                .limit(6)
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    public PaginatedResponse<BookSummaryDTO> getMostPopularWithLibraryInfo(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findMostPopular(pageable);

        // Obtener la biblioteca del usuario si es bibliotecario
        final Long libraryId;
        if (userId != null) {
            libraryId = librarianRepository.findByUserId(userId)
                    .map(librarian -> librarian.getLibrary().getId())
                    .orElse(null);
        } else {
            libraryId = null;
        }

        return toPaginatedResponseFromBooks(bookPage, book -> {
            if (libraryId != null) {
                // Verificar si el usuario ha añadido este libro a su biblioteca
                Optional<LibraryBook> libraryBook = libraryBookRepository
                        .findByLibraryIdAndBookId(libraryId, book.getId());
                Long userAddedId = libraryBook.map(lb -> lb.getUserAdded().getId()).orElse(null);
                return toSummaryDTOWithLibraryInfo(book, libraryId, userAddedId);
            }
            return toSummaryDTO(book);
        });
    }

    public PaginatedResponse<BookSummaryDTO> searchBooksByKeywordsAndLibraries(List<String> keywords,
            List<Long> libraryIds, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<Book> spec = Specification.where(null);

        if (keywords != null && !keywords.isEmpty()) {
            spec = spec.and(BookSpecifications.containsKeywords(keywords));
        }

        if (libraryIds != null && !libraryIds.isEmpty()) {
            spec = spec.and(BookSpecifications.inLibraries(libraryIds));
        }

        @SuppressWarnings("null")
        Page<Book> bookPage = bookRepository.findAll(spec, pageable);

        return toPaginatedResponseFromBooks(bookPage, this::toSummaryDTO);
    }

    @SuppressWarnings("null")
    public BookDetailDTO getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Libro no encontrado con ID: " + id));
        return toDetailDTO(book);
    }

    public BookDetailDTO getBookByIsbn(Long isbn) {
        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new RuntimeException("Libro no encontrado con ISBN: " + isbn));
        return toDetailDTO(book);
    }

    public void removeBookFromLibrary(Long bookId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getRole() != UserRole.LIBRARIAN) {
            throw new UnauthorizedException("Solo bibliotecarios pueden remover libros de su biblioteca");
        }

        Librarian librarian = librarianRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Perfil de bibliotecario no encontrado"));

        LibraryBook libraryBook = libraryBookRepository.findByLibraryIdAndBookId(librarian.getLibrary().getId(), bookId)
                .orElseThrow(() -> new RuntimeException("El libro no está en tu biblioteca"));

        libraryBookRepository.delete(libraryBook);
        entityManager.flush(); // Asegurar que el delete se persista antes de contar

        // Si el creador original renuncia al libro en su biblioteca, pierde los
        // permisos permanentemente
        Book book = libraryBook.getBook();
        if (book.getCreatedBy() != null && book.getCreatedBy().getId().equals(userId)) {
            book.setCreatedBy(null);
            bookRepository.save(book);
            entityManager.flush(); // Asegurar que el cambio se persista
        }

        // Si no hay más bibliotecas que tengan este libro, eliminarlo completamente de
        // la DB
        long libraryBooksCount = libraryBookRepository.countByBookId(bookId);
        if (libraryBooksCount == 0) {
            bookRepository.deleteById(bookId);
            entityManager.flush(); // Asegurar que el delete del libro se persista
        }
    }

    public void deleteBook(Long bookId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Solo administradores pueden eliminar libros del sistema");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Libro no encontrado"));

        bookRepository.delete(book);
    }

    public BookDetailDTO updateBook(Long bookId, BookPublishRequestDTO request, Long userId) {
        // Validar ISBN
        if (!ISBNValidator.isValid(String.valueOf(request.getIsbn()))) {
            throw new RuntimeException(ISBNValidator.getErrorMessage(String.valueOf(request.getIsbn())));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Libro no encontrado"));

        boolean isCreator = book.getCreatedBy() != null && book.getCreatedBy().getId().equals(userId);
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isCreator && !isAdmin) {
            throw new UnauthorizedException(
                    "No tienes permisos para editar este libro. Solo el creador original o un administrador pueden editarlo.");
        }

        book.setTitle(request.getTitle());
        book.setDescription(request.getDescription());
        book.setCoverUrl(request.getCoverUrl());
        book.setPublicationDate(request.getPublicationDate());
        book.setPages(request.getPages());

        // Autores
        if (request.getAuthors() != null) {
            List<Author> authors = request.getAuthors().stream()
                    .map(name -> authorRepository.findByName(name)
                            .orElseGet(() -> {
                                Author newAuthor = new Author();
                                newAuthor.setName(name);
                                return authorRepository.save(newAuthor);
                            }))
                    .collect(Collectors.toList());
            book.setAuthors(authors);
        }

        // Géneros: SOLO permitir géneros existentes
        if (request.getGenres() != null) {
            List<Genre> genres = request.getGenres().stream()
                    .map(name -> genreRepository.findByName(name.trim())
                            .orElseThrow(() -> new RuntimeException("El género '" + name
                                    + "' no existe en la plataforma. Por favor, usa solo los géneros permitidos.")))
                    .collect(Collectors.toList());
            book.setGenres(genres);
        }

        // Editoriales
        if (request.getPublishers() != null) {
            List<Publisher> publishers = request.getPublishers().stream()
                    .map(name -> publisherRepository.findByName(name)
                            .orElseGet(() -> {
                                Publisher newPublisher = new Publisher();
                                newPublisher.setName(name);
                                return publisherRepository.save(newPublisher);
                            }))
                    .collect(Collectors.toList());
            book.setPublishers(publishers);
        }

        bookRepository.save(book);

        // Actualizar disponibilidad en la biblioteca del usuario si es bibliotecario
        Librarian librarian = librarianRepository.findByUserId(userId).orElse(null);
        if (librarian != null && request.getAvailability() != null) {
            Optional<LibraryBook> lbOpt = libraryBookRepository.findByLibraryIdAndBookId(librarian.getLibrary().getId(),
                    book.getId());
            if (lbOpt.isPresent()) {
                LibraryBook lb = lbOpt.get();
                lb.setPhysicalCopies(request.getAvailability().getPhysicalCopies());
                lb.setDigitalAvailable(request.getAvailability().isDigital());
                libraryBookRepository.save(lb);
            }
        }

        return toDetailDTO(book);
    }

    // ========== MÉTODOS DE MAPEO ==========

    private BookSummaryDTO toSummaryDTO(Book book) {
        List<String> authorNames = book.getAuthors() != null
                ? book.getAuthors().stream().map(Author::getName).collect(Collectors.toList())
                : List.of();
        List<GenreDTO> genreDTOs = book.getGenres() != null
                ? book.getGenres().stream()
                        .map(g -> new GenreDTO(g.getId(), g.getName(), g.getDescription()))
                        .collect(Collectors.toList())
                : List.of();

        BookSummaryDTO dto = new BookSummaryDTO(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                authorNames,
                genreDTOs,
                book.getAverageRating() != null ? book.getAverageRating() : BigDecimal.ZERO,
                book.getRatingsCount() != null ? book.getRatingsCount() : 0,
                book.getCoverUrl(),
                null, null, book.getCreatedBy() != null ? book.getCreatedBy().getId() : null);

        if (book.getLibraryBooks() != null) {
            List<String> libNames = book.getLibraryBooks().stream()
                    .map(lb -> lb.getLibrary().getName())
                    .distinct()
                    .collect(Collectors.toList());
            dto.setAvailableLibraries(libNames);
        }

        return dto;
    }

    private List<BookSummaryDTO> listToSummary(List<Book> books) {
        return books.stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    private String normalizeBlank(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private BookSummaryDTO toSummaryDTOWithLibraryInfo(Book book, Long libraryId, Long userAddedId) {
        BookSummaryDTO dto = toSummaryDTO(book);
        dto.setLibraryId(libraryId);
        dto.setUserAddedId(userAddedId);
        return dto;
    }

    private BookDetailDTO toDetailDTO(Book book) {
        List<String> authorNames = book.getAuthors() != null
                ? book.getAuthors().stream().map(Author::getName).collect(Collectors.toList())
                : List.of();

        List<GenreDTO> genreDTOs = book.getGenres() != null
                ? book.getGenres().stream()
                        .map(g -> new GenreDTO(g.getId(), g.getName(), g.getDescription()))
                        .collect(Collectors.toList())
                : List.of();

        List<String> publisherNames = book.getPublishers() != null
                ? book.getPublishers().stream().map(Publisher::getName).collect(Collectors.toList())
                : List.of();

        List<String> formatNames = book.getFormats() != null
                ? book.getFormats().stream()
                        .map(bf -> bf.getFormat() != null ? bf.getFormat().getName() : null)
                        .filter(name -> name != null)
                        .collect(Collectors.toList())
                : List.of();

        BookDetailDTO dto = new BookDetailDTO(
                book.getId(),
                book.getTitle(),
                authorNames,
                genreDTOs,
                book.getAverageRating() != null ? book.getAverageRating() : BigDecimal.ZERO,
                book.getRatingsCount() != null ? book.getRatingsCount() : 0,
                book.getCoverUrl(),
                book.getDescription(),
                publisherNames,
                book.getPublicationDate(),
                book.getPages(),
                book.getIsbn(),
                formatNames);

        if (book.getLibraryBooks() != null) {
            List<com.lectuaria.backend.dto.library.LibraryAvailabilityDTO> availabilityList = book.getLibraryBooks()
                    .stream()
                    .map(lb -> new com.lectuaria.backend.dto.library.LibraryAvailabilityDTO(
                            new com.lectuaria.backend.dto.library.LibrarySummaryDTO(
                                    lb.getLibrary().getId(),
                                    lb.getLibrary().getName(),
                                    lb.getLibrary().getDescription(),
                                    lb.getLibrary().getAddress(),
                                    lb.getLibrary().getContactEmail(),
                                    lb.getLibrary().getContactPhone(),
                                    lb.getLibrary().getOpeningHours(),
                                    null),
                            lb.getPhysicalCopies() != null && lb.getPhysicalCopies() > 0,
                            lb.getPhysicalCopies(),
                            lb.getDigitalAvailable() != null && lb.getDigitalAvailable(),
                            lb.getDigitalPlatform()))
                    .collect(Collectors.toList());
            dto.setAvailability(availabilityList);
        }

        return dto;
    }

    private <T> PaginatedResponse<T> toPaginatedResponseFromBooks(Page<Book> page,
            java.util.function.Function<Book, T> mapper) {
        List<T> content = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious());
    }

    private <T> PaginatedResponse<T> toPaginatedResponseFromLibraryBooks(Page<LibraryBook> page,
            java.util.function.Function<LibraryBook, T> mapper) {
        List<T> content = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious());
    }

    /**
     * Sincroniza las estadísticas de calificación cuando se actualiza directamente
     * un Book
     * Este método debe llamarse después de cualquier actualización directa de
     * Book.averageRating o Book.ratingsCount
     */
    public void syncRatingStatsFromBook(Book book) {
        if (bookRatingService != null) {
            bookRatingService.syncBookRatingStatsFromBook(book);
        }
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<BookSummaryDTO> getBooksByFormatAvailability(String formatType, int page, int size,
            Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findByFormatAvailability(formatType, pageable);
        return toPaginatedResponseFromBooks(bookPage, this::toSummaryDTO);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<BookSummaryDTO> searchBooksByMultipleFilters(BookFilterDTO filter, int page, int size,
            Long userId) {
        Pageable pageable = PageRequest.of(page, size);

        String keywords = null;
        if (filter.getKeywords() != null && !filter.getKeywords().isEmpty()) {
            keywords = String.join(" ", filter.getKeywords());
            // Treat empty string as null to avoid SQL error
            if (keywords.trim().isEmpty()) {
                keywords = null;
            }
        }

        // Convert empty lists to null for proper JPA query handling
        List<Long> genreIds = (filter.getGenreIds() != null && !filter.getGenreIds().isEmpty())
                ? filter.getGenreIds()
                : null;
        List<Long> libraryIds = (filter.getLibraryIds() != null && !filter.getLibraryIds().isEmpty())
                ? filter.getLibraryIds()
                : null;
        List<String> formatTypes = (filter.getFormatTypes() != null && !filter.getFormatTypes().isEmpty())
                ? filter.getFormatTypes()
                : null;

        Page<Book> bookPage = bookRepository.searchBooksByMultipleFilters(
                keywords,
                genreIds,
                libraryIds,
                formatTypes,
                filter.getMinYear(),
                filter.getMaxYear(),
                filter.getMinRating(),
                pageable);

        return toPaginatedResponseFromBooks(bookPage, this::toSummaryDTO);
    }
}

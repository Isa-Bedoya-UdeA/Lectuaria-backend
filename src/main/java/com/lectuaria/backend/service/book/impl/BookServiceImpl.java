package com.lectuaria.backend.service.book.impl;

import com.lectuaria.backend.dto.book.*;
import com.lectuaria.backend.service.book.IBookService;
import com.lectuaria.backend.service.book.IBookRatingService;
import com.lectuaria.backend.service.storage.S3StorageService;
import com.lectuaria.backend.dto.common.PaginatedResponse;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.BookEditHistory;
import com.lectuaria.backend.model.book.Genre;
import com.lectuaria.backend.model.book.LibraryBook;
import com.lectuaria.backend.model.book.Publisher;
import com.lectuaria.backend.repository.book.AuthorRepository;
import com.lectuaria.backend.repository.book.BookEditHistoryRepository;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.book.PlatformRepository;
import com.lectuaria.backend.repository.book.GenreRepository;
import com.lectuaria.backend.repository.library.LibraryBookRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.repository.book.PublisherRepository;
import com.lectuaria.backend.model.book.Platform;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.specification.BookSpecifications;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.util.ISBNValidator;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.library.Librarian;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookServiceImpl implements IBookService {

    private final BookRepository bookRepository;
    private final BookEditHistoryRepository bookEditHistoryRepository;
    private final PlatformRepository platformRepository;
    private final LibraryBookRepository libraryBookRepository;
    private final LibrarianRepository librarianRepository;
    private final IBookRatingService bookRatingService;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final PublisherRepository publisherRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final UserListBookRepository listBookRepository;
    private final S3StorageService s3StorageService;
    private final ObjectMapper objectMapper;

    public BookServiceImpl(BookRepository bookRepository,
            BookEditHistoryRepository bookEditHistoryRepository,
            PlatformRepository platformRepository,
            AuthorRepository authorRepository,
            GenreRepository genreRepository,
            PublisherRepository publisherRepository,
            LibraryBookRepository libraryBookRepository,
            LibrarianRepository librarianRepository,
            IBookRatingService bookRatingService,
            UserRepository userRepository,
            EntityManager entityManager,
            UserListBookRepository listBookRepository,
            S3StorageService s3StorageService,
            ObjectMapper objectMapper) {
        this.bookRepository = bookRepository;
        this.bookEditHistoryRepository = bookEditHistoryRepository;
        this.platformRepository = platformRepository;
        this.libraryBookRepository = libraryBookRepository;
        this.librarianRepository = librarianRepository;
        this.bookRatingService = bookRatingService;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.publisherRepository = publisherRepository;
        this.entityManager = entityManager;
        this.userRepository = userRepository;
        this.listBookRepository = listBookRepository;
        this.s3StorageService = s3StorageService;
        this.objectMapper = objectMapper;
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
                Instant createdAt = libraryBook.map(lb -> lb.getAddedAt()).orElse(book.getCreatedAt());
                return toSummaryDTOWithLibraryInfo(book, libraryId, userAddedId, createdAt);
            }
            return toSummaryDTO(book);
        });
    }

    public PaginatedResponse<BookSummaryDTO> getBooksByGenre(Long genreId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findByGenreId(genreId, pageable);
        return toPaginatedResponseFromBooks(bookPage, this::toSummaryDTO);
    }

    public PaginatedResponse<BookSummaryDTO> getBooksByLibrary(Long libraryId, int page, int size, String keyword, String sort) {
        Pageable pageable = buildPageable(sort, page, size);

        Specification<LibraryBook> spec = (root, query, cb) -> {
            query.distinct(true);
            Join<LibraryBook, Book> bookJoin = root.join("book", JoinType.INNER);
            Join<Book, Author> authorJoin = bookJoin.join("authors", JoinType.LEFT);
            Predicate libraryMatch = cb.equal(root.get("library").get("id"), libraryId);

            if (keyword != null && !keyword.trim().isEmpty()) {
                List<String> words = Arrays.stream(keyword.trim().toLowerCase().split("\\s+"))
                        .map(String::trim)
                        .filter(w -> !w.isEmpty())
                        .collect(Collectors.toList());
                if (!words.isEmpty()) {
                    List<Predicate> wordPredicates = new java.util.ArrayList<>();
                    for (String w : words) {
                        String pattern = "%" + w + "%";
                        wordPredicates.add(cb.or(
                                cb.like(cb.lower(bookJoin.get("title")), pattern),
                                cb.like(cb.lower(authorJoin.get("name")), pattern)));
                    }
                    return cb.and(libraryMatch, cb.or(wordPredicates.toArray(new Predicate[0])));
                }
            }
            return libraryMatch;
        };

        Page<LibraryBook> libraryBookPage = libraryBookRepository.findAll(spec, pageable);

        // Manual re-sorting to prioritize exact matches (title > author > partial)
        List<LibraryBook> sortedBooks = new ArrayList<>(libraryBookPage.getContent());
        if (keyword != null && !sortedBooks.isEmpty()) {
            List<String> words = Arrays.stream(keyword.toLowerCase().split("\\s+"))
                    .map(String::trim)
                    .filter(w -> !w.isEmpty())
                    .collect(Collectors.toList());
            if (!words.isEmpty()) {
                sortedBooks.sort((lb1, lb2) -> {
                    Book b1 = lb1.getBook();
                    Book b2 = lb2.getBook();
                    int score1 = getMatchScoreForLibraryBook(b1, words);
                    int score2 = getMatchScoreForLibraryBook(b2, words);
                    if (score1 != score2) return Integer.compare(score1, score2);
                    BigDecimal r1 = b1.getAverageRating();
                    BigDecimal r2 = b2.getAverageRating();
                    int cmp = (r2 != null ? r2 : BigDecimal.ZERO).compareTo(r1 != null ? r1 : BigDecimal.ZERO);
                    if (cmp != 0) return cmp;
                    Integer c1 = b1.getRatingsCount() != null ? b1.getRatingsCount() : 0;
                    Integer c2 = b2.getRatingsCount() != null ? b2.getRatingsCount() : 0;
                    return Integer.compare(c2, c1);
                });
            }
        }

        List<BookSummaryDTO> content = sortedBooks.stream()
                .map(lb -> {
                    Long userAddedId = lb.getUserAdded() != null ? lb.getUserAdded().getId() : null;
                    return toSummaryDTOWithLibraryInfo(lb.getBook(), libraryId, userAddedId, lb.getAddedAt());
                })
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                libraryBookPage.getNumber(),
                libraryBookPage.getSize(),
                libraryBookPage.getTotalElements(),
                libraryBookPage.getTotalPages(),
                libraryBookPage.isFirst(),
                libraryBookPage.isLast(),
                libraryBookPage.hasNext(),
                libraryBookPage.hasPrevious()
        );
    }

    private int getMatchScoreForLibraryBook(Book book, List<String> words) {
        String title = book.getTitle() != null ? book.getTitle().toLowerCase() : "";
        String authorNames = book.getAuthors() != null
                ? book.getAuthors().stream()
                        .map(a -> a.getName() != null ? a.getName().toLowerCase() : "")
                        .collect(Collectors.joining(" "))
                : "";
        int score = 100;
        for (String w : words) {
            if (words.size() == 1 && title.equals(w)) return 0;
            if (title.equals(w)) return Math.min(score, 1);
            if (words.size() == 1 && authorNames.contains(w)) return 10;
            if (authorNames.contains(w)) return Math.min(score, 11);
            if (title.contains(w)) return Math.min(score, 20);
            if (authorNames.contains(w)) return Math.min(score, 21);
        }
        return score;
    }

    private Pageable buildPageable(String sort, int page, int size) {
        if (sort == null || sort.isEmpty() || "none".equals(sort)) {
            return PageRequest.of(page, size);
        }
        Sort.Order order = switch (sort) {
            case "title_asc" -> Sort.Order.asc("book.title");
            case "title_desc" -> Sort.Order.desc("book.title");
            case "newest" -> Sort.Order.desc("addedAt");
            case "oldest" -> Sort.Order.asc("addedAt");
            default -> Sort.Order.desc("addedAt");
        };
        return PageRequest.of(page, size, Sort.by(order));
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
                Instant createdAt = libraryBook.map(lb -> lb.getAddedAt()).orElse(book.getCreatedAt());
                return toSummaryDTOWithLibraryInfo(book, libraryId, userAddedId, createdAt);
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

    public PaginatedResponse<BookCatalogItemDTO> getNewCatalogBooks(int page, int size, Long genreId) {
        Pageable pageable = PageRequest.of(page, size);
        Instant since = Instant.now().minus(java.time.Duration.ofDays(30));
        Page<Book> bookPage = bookRepository.findNewCatalogBooks(since, genreId, pageable);
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
                Instant createdAt = libraryBook.map(lb -> lb.getAddedAt()).orElse(book.getCreatedAt());
                return toSummaryDTOWithLibraryInfo(book, libraryId, userAddedId, createdAt);
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
        // la DB Y de S3 storage
        long libraryBooksCount = libraryBookRepository.countByBookId(bookId);
        if (libraryBooksCount == 0) {
            // Delete cover from S3 if exists
            if (book.getCoverUrl() != null && !book.getCoverUrl().isBlank()) {
                s3StorageService.deleteCover(book.getCoverUrl());
            }
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

        // Capturar estado anterior para historial
        String oldData = toBookJson(book);

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

        // No longer using book_format tables (removed)

        bookRepository.save(book);

        // Guardar historial de edición
        String newData = toBookJson(book);
        saveEditHistory(book, user, oldData, newData);

        // Actualizar disponibilidad en la biblioteca del usuario si es bibliotecario
        Librarian librarian = librarianRepository.findByUserId(userId).orElse(null);
        if (librarian != null && request.getAvailability() != null) {
            Optional<LibraryBook> lbOpt = libraryBookRepository.findByLibraryIdAndBookId(librarian.getLibrary().getId(),
                    book.getId());
            if (lbOpt.isPresent()) {
                LibraryBook lb = lbOpt.get();
                lb.setPhysicalCopies(request.getAvailability().getPhysicalCopies());
                lb.setDigitalAvailable(request.getAvailability().isDigital());
                lb.setDigitalPlatform(request.getPlatformId());
                libraryBookRepository.save(lb);
            }
        }

        return toDetailDTO(book);
    }

    private String toBookJson(Book book) {
        try {
            return objectMapper.writeValueAsString(book);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void saveEditHistory(Book book, User user, String oldValueJson, String newValueJson) {
        Librarian librarian = librarianRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Librarian no encontrado para este usuario"));

        BookEditHistory history = new BookEditHistory();
        history.setBook(book);
        history.setLibrary(librarian.getLibrary());
        history.setLibrarian(librarian);
        history.setOldValueJson(oldValueJson);
        history.setNewValueJson(newValueJson);
        bookEditHistoryRepository.save(history);
    }

    // No longer using book_format tables (removed)

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
                null, null, book.getCreatedBy() != null ? book.getCreatedBy().getId() : null,
                book.getCreatedAt());

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

    private BookSummaryDTO toSummaryDTOWithLibraryInfo(Book book, Long libraryId, Long userAddedId, Instant createdAt) {
        BookSummaryDTO dto = toSummaryDTO(book);
        dto.setLibraryId(libraryId);
        dto.setUserAddedId(userAddedId);
        dto.setCreatedAt(createdAt);
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
                List.of());

        if (book.getLibraryBooks() != null) {
            List<com.lectuaria.backend.dto.library.LibraryAvailabilityDTO> availabilityList = book.getLibraryBooks()
                    .stream()
                    .map(lb -> {
                        Long platformId = lb.getDigitalPlatform();
                        String platformName = null;
                        if (platformId != null) {
                            platformName = platformRepository.findById(platformId)
                                    .map(Platform::getName)
                                    .orElse(null);
                        }
                        var avail = new com.lectuaria.backend.dto.library.LibraryAvailabilityDTO(
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
                                platformName);
                        avail.setPlatformId(platformId);
                        return avail;
                    })
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

// Build search string from keywords — each word must match (OR across title/author/genre)
        String keywords = null;
        if (filter.getKeywords() != null && !filter.getKeywords().isEmpty()) {
            List<String> words = filter.getKeywords().stream()
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .collect(Collectors.toList());
            if (!words.isEmpty()) {
                keywords = String.join(" ", words);
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

        // Map formatTypes list to hasPhysical/hasDigital booleans
        Boolean hasPhysical = null;
        Boolean hasDigital = null;
        if (formatTypes != null) {
            hasPhysical = formatTypes.contains("physical");
            hasDigital = formatTypes.contains("digital");
        }

        // Build Specification for multi-keyword OR logic
        Specification<Book> spec = Specification.where(null);

        if (keywords != null && !keywords.isEmpty()) {
            List<String> wordList = Arrays.asList(keywords.split("\\s+")).stream()
                    .map(String::trim)
                    .filter(w -> !w.isEmpty())
                    .collect(Collectors.toList());
            if (!wordList.isEmpty()) {
                final List<String> words = wordList;
                spec = spec.and((root, query, cb) -> {
                    query.distinct(true);
                    Join<Book, Author> authorJoin = root.join("authors", JoinType.LEFT);
                    Join<Book, Genre> genreJoin = root.join("genres", JoinType.LEFT);
                    // Single OR predicate across all words
                    List<Predicate> wordPredicates = new ArrayList<>();
                    for (String w : words) {
                        String lower = w.toLowerCase();
                        wordPredicates.add(cb.or(
                                cb.like(cb.lower(root.get("title")), "%" + lower + "%"),
                                cb.like(cb.lower(authorJoin.get("name")), "%" + lower + "%"),
                                cb.like(cb.lower(genreJoin.get("name")), "%" + lower + "%")
                        ));
                    }
                    return cb.or(wordPredicates.toArray(new Predicate[0]));
                });
            }
        }

        if (genreIds != null) {
            spec = spec.and((root, query, cb) -> {
                Join<Book, Genre> genreJoin = root.join("genres", JoinType.LEFT);
                return cb.in(genreJoin.get("id")).value(genreIds);
            });
        }

        if (libraryIds != null) {
            spec = spec.and((root, query, cb) -> {
                Join<Book, LibraryBook> lbJoin = root.join("libraryBooks", JoinType.LEFT);
                return cb.in(lbJoin.get("library").get("id")).value(libraryIds);
            });
        }

        if (hasPhysical != null || hasDigital != null) {
            final Boolean hp = hasPhysical;
            final Boolean hd = hasDigital;
            spec = spec.and((root, query, cb) -> {
                Join<Book, LibraryBook> lbJoin = root.join("libraryBooks", JoinType.LEFT);
                List<Predicate> conditions = new ArrayList<>();
                if (hp != null && hp) {
                    conditions.add(cb.gt(lbJoin.get("physicalCopies"), 0));
                }
                if (hd != null && hd) {
                    conditions.add(cb.isTrue(lbJoin.get("digitalAvailable")));
                }
                return conditions.stream().reduce(cb.conjunction(), cb::or);
            });
        }

        if (filter.getMinYear() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(
                            cb.function("date_part", Integer.class, cb.literal("year"), root.get("publicationDate")),
                            filter.getMinYear())
            );
        }

        if (filter.getMaxYear() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(
                            cb.function("date_part", Integer.class, cb.literal("year"), root.get("publicationDate")),
                            filter.getMaxYear())
            );
        }

        if (filter.getMinRating() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("averageRating"), filter.getMinRating()));
        }

Page<Book> bookPage = bookRepository.findAll(spec, pageable);

        // Manual re-sorting to prioritize exact matches (title > author > partial)
        List<Book> sortedBooks = new ArrayList<>(bookPage.getContent());
        if (keywords != null && !sortedBooks.isEmpty()) {
            List<String> words = Arrays.asList(keywords.toLowerCase().split("\\s+")).stream()
                    .map(String::trim)
                    .filter(w -> !w.isEmpty())
                    .collect(Collectors.toList());
            if (!words.isEmpty()) {
                sortedBooks.sort((b1, b2) -> {
                    int score1 = getMatchScore(b1, words);
                    int score2 = getMatchScore(b2, words);
                    if (score1 != score2) return Integer.compare(score1, score2);
                    // Secondary: higher rating more reviews first
                    BigDecimal r1 = b1.getAverageRating();
                    BigDecimal r2 = b2.getAverageRating();
                    int cmp = (r2 != null ? r2 : BigDecimal.ZERO).compareTo(r1 != null ? r1 : BigDecimal.ZERO);
                    if (cmp != 0) return cmp;
                    Integer c1 = b1.getRatingsCount() != null ? b1.getRatingsCount() : 0;
                    Integer c2 = b2.getRatingsCount() != null ? b2.getRatingsCount() : 0;
                    return Integer.compare(c2, c1);
                });
            }
        }

        return toPaginatedResponseFromBooks(new org.springframework.data.domain.PageImpl<>(
                sortedBooks, pageable, bookPage.getTotalElements()), this::toSummaryDTO);
    }

    private int getMatchScore(Book book, List<String> words) {
        String title = book.getTitle() != null ? book.getTitle().toLowerCase() : "";
        String authorNames = book.getAuthors() != null
                ? book.getAuthors().stream()
                        .map(a -> a.getName() != null ? a.getName().toLowerCase() : "")
                        .collect(Collectors.joining(" "))
                : "";
        int score = 100;
        for (String w : words) {
            // Exact title match = best
            if (words.size() == 1 && title.equals(w)) return 0;
            if (title.equals(w)) return Math.min(score, 1);
            // Exact author match
            if (words.size() == 1 && authorNames.contains(w)) return 10;
            if (authorNames.contains(w)) return Math.min(score, 11);
            // Partial title match
            if (title.contains(w)) return Math.min(score, 20);
            // Partial author match
            if (authorNames.contains(w)) return Math.min(score, 21);
            score++;
        }
        return score;
    }
}

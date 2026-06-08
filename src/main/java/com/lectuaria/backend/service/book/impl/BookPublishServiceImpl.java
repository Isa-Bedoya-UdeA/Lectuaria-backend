package com.lectuaria.backend.service.book.impl;

import com.lectuaria.backend.dto.book.*;
import com.lectuaria.backend.service.book.IBookPublishService;
import com.lectuaria.backend.dto.book.externalApi.ExternalBookMetadataDTO;
import com.lectuaria.backend.exception.BookAlreadyExistsInLibraryException;
import com.lectuaria.backend.exception.ForbiddenException;
import com.lectuaria.backend.exception.ResourceNotFoundException;
import com.lectuaria.backend.exception.ValidationException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.Genre;
import com.lectuaria.backend.model.book.LibraryBook;
import com.lectuaria.backend.model.book.Publisher;
import com.lectuaria.backend.model.library.Librarian;
import com.lectuaria.backend.model.library.Library;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.repository.library.LibraryBookRepository;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.book.PlatformRepository;
import com.lectuaria.backend.repository.book.AuthorRepository;
import com.lectuaria.backend.repository.book.GenreRepository;
import com.lectuaria.backend.repository.book.PublisherRepository;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.model.book.Platform;
import com.lectuaria.backend.service.book.externalApi.IExternalBookMetadataService;
import com.lectuaria.backend.util.ISBNValidator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookPublishServiceImpl implements IBookPublishService {

    private final BookRepository bookRepository;
    private final PlatformRepository platformRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final PublisherRepository publisherRepository;
    private final LibrarianRepository librarianRepository;
    private final LibraryBookRepository libraryBookRepository;
    private final UserRepository userRepository;
    private final IExternalBookMetadataService externalBookMetadataService;

    public BookPublishServiceImpl(BookRepository bookRepository,
            PlatformRepository platformRepository,
            AuthorRepository authorRepository,
            GenreRepository genreRepository,
            PublisherRepository publisherRepository,
            LibrarianRepository librarianRepository,
            LibraryBookRepository libraryBookRepository,
            UserRepository userRepository,
            IExternalBookMetadataService externalBookMetadataService) {
        this.bookRepository = bookRepository;
        this.platformRepository = platformRepository;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.publisherRepository = publisherRepository;
        this.librarianRepository = librarianRepository;
        this.libraryBookRepository = libraryBookRepository;
        this.userRepository = userRepository;
        this.externalBookMetadataService = externalBookMetadataService;
    }

    @SuppressWarnings("null")
    @Transactional
    public BookPublishResponseDTO publishBook(BookPublishRequestDTO request, Long librarianUserId) {
        // 0. Validar ISBN
        if (!ISBNValidator.isValid(String.valueOf(request.getIsbn()))) {
            throw new ValidationException(ISBNValidator.getErrorMessage(String.valueOf(request.getIsbn())));
        }

        // 1. Verificar que el usuario existe y es bibliotecario
        @SuppressWarnings("null")
        User librarianUser = userRepository.findById(librarianUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + librarianUserId));

        if (librarianUser.getRole() != UserRole.LIBRARIAN) {
            throw new ForbiddenException("Solo los bibliotecarios pueden publicar libros");
        }

        // 2. Obtener la biblioteca del bibliotecario
        Librarian librarian = librarianRepository.findByUser(librarianUser)
                .orElseThrow(() -> new ResourceNotFoundException("Este usuario no tiene una biblioteca asociada"));

        Library library = librarian.getLibrary();

        // 3. Verificar si el libro YA está en esta biblioteca (evitar duplicados)
        Optional<Book> existingBook = bookRepository.findByIsbn(request.getIsbn());
        if (existingBook.isPresent()
                && libraryBookRepository.existsByLibraryIdAndBookId(library.getId(), existingBook.get().getId())) {
            throw new BookAlreadyExistsInLibraryException("Este libro ya está registrado en tu biblioteca.");
        }

        // 4. Buscar libro por ISBN en el catálogo global
        boolean isNewBook = existingBook.isEmpty();
        Book book;

        if (isNewBook) {
            // 5. Crear nuevo libro en el catálogo global
            book = createBookFromRequest(request, librarianUser);
            bookRepository.save(book);
        } else {
            // 6. Libro ya existe, usar el existente
            book = existingBook.get();
        }

        // 7. Asociar libro a la biblioteca (tabla LIBRARY_BOOK)
        associateBookToLibrary(book, library, request.getAvailability(), librarianUserId, request.getPlatformId());

        String message = isNewBook
                ? "Libro creado y añadido a tu biblioteca exitosamente."
                : "Libro ya existía en Lectuaria, añadido a tu biblioteca exitosamente.";

        return new BookPublishResponseDTO(book.getId(), book.getTitle(), book.getIsbn(), isNewBook, message);
    }

    private Book createBookFromRequest(BookPublishRequestDTO request, User createdBy) {
        Book book = new Book();
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setDescription(request.getDescription());
        book.setCoverUrl(request.getCoverUrl());
        book.setPublicationDate(request.getPublicationDate());
        book.setPages(request.getPages());
        book.setAverageRating(BigDecimal.ZERO);
        book.setRatingsCount(0);
        book.setCreatedBy(createdBy); // Establecer el creador del libro

        // Autores: buscar o crear
        List<Author> authors = request.getAuthors().stream()
                .map(name -> authorRepository.findByName(name)
                        .orElseGet(() -> {
                            Author newAuthor = new Author();
                            newAuthor.setName(name);
                            return authorRepository.save(newAuthor);
                        }))
                .collect(Collectors.toList());
        book.setAuthors(authors);

        // Géneros: SOLO permitir géneros existentes — recolectar todos los errores
        List<String> genreErrors = new ArrayList<>();
        List<Genre> genres = new ArrayList<>();
        for (String name : request.getGenres()) {
            Optional<Genre> genreOpt = genreRepository.findByName(name.trim());
            if (genreOpt.isPresent()) {
                genres.add(genreOpt.get());
            } else {
                genreErrors.add("el género '" + name + "' no existe en la plataforma");
            }
        }
        if (!genreErrors.isEmpty()) {
            throw new ValidationException(
                    "Géneros no válidos: " + String.join("; ", genreErrors) + ". Usa solo los géneros permitidos.");
        }
        book.setGenres(genres);

        // Editoriales: buscar o crear
        List<Publisher> publishers = request.getPublishers().stream()
                .map(name -> publisherRepository.findByName(name)
                        .orElseGet(() -> {
                            Publisher newPublisher = new Publisher();
                            newPublisher.setName(name);
                            return publisherRepository.save(newPublisher);
                        }))
                .collect(Collectors.toList());
        book.setPublishers(publishers);

        return book;
    }

    private void associateBookToLibrary(Book book, Library library,
            AvailabilityDTO availability, Long userId, Long platformId) {
        if (libraryBookRepository.existsByLibraryIdAndBookId(library.getId(), book.getId())) {
            throw new BookAlreadyExistsInLibraryException(
                    "Este libro ya está registrado en tu biblioteca.");
        }

        LibraryBook libraryBook = new LibraryBook();
        libraryBook.setLibrary(library);
        libraryBook.setBook(book);
        libraryBook.setPhysicalCopies(availability.isPhysical() ? availability.getPhysicalCopies() : 0);
        libraryBook.setDigitalAvailable(availability.isDigital());
        libraryBook.setDigitalPlatform(platformId);
        libraryBook.setUserAdded(userId != null ? userRepository.findById(userId).orElse(null) : null);

        libraryBookRepository.save(libraryBook);
    }

    // Método: Pre-llenar datos desde Catálogo Local o OpenLibrary
    @Transactional(readOnly = true)
    public BookPublishRequestDTO prefillFromOpenLibrary(@NonNull Long isbn, Long librarianUserId) {
        // 1. Verificar si el libro ya existe en el catálogo global de Lectuaria
        Optional<Book> localBook = bookRepository.findByIsbn(isbn);

        // 2. Obtener la biblioteca del bibliotecario
        Librarian librarian = librarianRepository.findByUserId(librarianUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Bibliotecario no encontrado para el usuario: " + librarianUserId));
        Library library = librarian.getLibrary();

        if (localBook.isPresent()) {
            Book book = localBook.get();
            BookPublishRequestDTO request = new BookPublishRequestDTO();
            request.setIsbn(book.getIsbn());
            request.setTitle(book.getTitle());
            request.setDescription(book.getDescription());
            request.setCoverUrl(book.getCoverUrl());
            request.setPages(book.getPages());
            request.setPublicationDate(book.getPublicationDate());
            request.setBookExistsInCatalog(true);

            // 3. Verificar si el libro ya existe en la biblioteca específica del usuario
            boolean existsInUserLibrary = libraryBookRepository.existsByLibraryIdAndBookId(library.getId(),
                    book.getId());
            request.setBookExistsInUserLibrary(existsInUserLibrary);

            request.setAuthors(book.getAuthors().stream()
                    .map(Author::getName)
                    .collect(Collectors.toList()));

            request.setPublishers(book.getPublishers().stream()
                    .map(Publisher::getName)
                    .collect(Collectors.toList()));

            request.setGenres(book.getGenres().stream()
                    .map(Genre::getName)
                    .collect(Collectors.toList()));

            return request;
        }

        // 2. Si no existe en Lectuaria, buscar en APIs externas (Google Books primero,
        // OpenLibrary fallback)
        ExternalBookMetadataDTO externalBook = externalBookMetadataService.fetchBookMetadata(isbn);

        if (externalBook == null) {
            // No se encontró en ninguna API, retornar request vacío
            BookPublishRequestDTO request = new BookPublishRequestDTO();
            request.setIsbn(isbn);
            request.setBookExistsInCatalog(false);
            request.setAuthors(new ArrayList<>());
            request.setPublishers(new ArrayList<>());
            request.setGenres(new ArrayList<>());
            return request;
        }

        BookPublishRequestDTO request = new BookPublishRequestDTO();
        request.setIsbn(isbn);
        request.setTitle(externalBook.getTitle());
        request.setDescription(externalBook.getDescription());
        request.setBookExistsInCatalog(false);

        // Autores
        request.setAuthors(externalBook.getAuthors() != null ? externalBook.getAuthors() : new ArrayList<>());

        // Editoriales
        if (externalBook.getPublisher() != null) {
            request.setPublishers(Arrays.asList(externalBook.getPublisher()));
        } else {
            request.setPublishers(new ArrayList<>());
        }

        // Géneros - por ahora vacíos hasta que se implementen géneros fijos
        request.setGenres(new ArrayList<>());

        // Páginas
        request.setPages(externalBook.getPageCount());

        // Fecha de publicación (parsear string a LocalDate)
        if (externalBook.getPublishedDate() != null) {
            try {
                String dateStr = externalBook.getPublishedDate();
                if (dateStr.length() >= 4) {
                    int year = Integer.parseInt(dateStr.substring(0, 4));
                    request.setPublicationDate(LocalDate.of(year, 1, 1));
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        // Portada
        request.setCoverUrl(externalBook.getCoverUrl());

        return request;
    }
}
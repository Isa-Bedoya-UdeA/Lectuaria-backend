// src/main/java/com/lectuaria/backend/controller/books/BookController.java

package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.BookCatalogItemDTO;
import com.lectuaria.backend.dto.book.BookDetailDTO;
import com.lectuaria.backend.dto.book.FeaturedSectionsDTO;
import com.lectuaria.backend.dto.book.BookFilterDTO;
import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.book.BookPublishRequestDTO;
import com.lectuaria.backend.dto.common.PaginatedResponse;
import com.lectuaria.backend.dto.common.ShareLinkDTO;
import com.lectuaria.backend.util.LinkGenerationUtil;

import com.lectuaria.backend.exception.UnauthorizedException;

import com.lectuaria.backend.model.auth.User;

import com.lectuaria.backend.model.auth.UserRole;

import com.lectuaria.backend.model.library.Librarian;

import com.lectuaria.backend.repository.auth.UserRepository;

import com.lectuaria.backend.repository.library.LibrarianRepository;

import com.lectuaria.backend.service.book.IBookService;

import java.util.Arrays;

import java.util.List;

import java.util.stream.Collectors;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController

@RequestMapping("/api/books")

public class BookController {
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);
    private final IBookService bookService;
    private final UserRepository userRepository;
    private final LibrarianRepository librarianRepository;

    public BookController(IBookService bookService,
            UserRepository userRepository,
            LibrarianRepository librarianRepository) {

        this.bookService = bookService;
        this.userRepository = userRepository;
        this.librarianRepository = librarianRepository;
    }

    // ========== BÚSQUEDA Y LISTADO ==========

    @GetMapping

    public ResponseEntity<PaginatedResponse<BookSummaryDTO>> getAllBooks(

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Float minRating,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) List<String> formatTypes) {

        // Get authenticated user or null if not authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;

        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }

        return ResponseEntity.ok(bookService.getAllBooks(page, size, minRating, startYear, endYear, formatTypes, userId));

    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<BookSummaryDTO>> searchBooks(
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) List<Long> genreIds,
            @RequestParam(required = false) List<Long> libraryIds,
            @RequestParam(required = false) List<String> formatTypes,
            @RequestParam(required = false) Integer minYear,
            @RequestParam(required = false) Integer maxYear,
            @RequestParam(required = false) Float minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }

        List<String> keywordList = null;
        if (keywords != null && !keywords.trim().isEmpty()) {
            keywordList = Arrays.stream(keywords.split("[,\\s]+"))
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .collect(Collectors.toList());
        }

        BookFilterDTO filter = new BookFilterDTO();
        filter.setKeywords(keywordList);
        filter.setGenreIds(genreIds);
        filter.setLibraryIds(libraryIds);
        filter.setFormatTypes(formatTypes);
        filter.setMinYear(minYear);
        filter.setMaxYear(maxYear);
        filter.setMinRating(minRating);

        return ResponseEntity.ok(bookService.searchBooksByMultipleFilters(filter, page, size, userId));
    }

    @GetMapping("/genre/{genreId}")

    public ResponseEntity<PaginatedResponse<BookSummaryDTO>> getBooksByGenre(

            @PathVariable Long genreId,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "12") int size) {

        return ResponseEntity.ok(bookService.getBooksByGenre(genreId, page, size));

    }

    @GetMapping("/filter/availability")
    public ResponseEntity<PaginatedResponse<BookSummaryDTO>> getBooksByFormatAvailability(
            @RequestParam String format,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }
        return ResponseEntity.ok(bookService.getBooksByFormatAvailability(format, page, size, userId));
    }

    @GetMapping("/library/{libraryId}")

    public ResponseEntity<PaginatedResponse<BookSummaryDTO>> getBooksByLibrary(
            @PathVariable Long libraryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        logger.info("BookController: getBooksByLibrary called with libraryId: {}", libraryId);

        // Obtener el usuario autenticado desde el contexto de seguridad de Spring
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("BookController: User is not authenticated for getBooksByLibrary");
            throw new UnauthorizedException("Usuario no autenticado");
        }

        String email = authentication.getName();
        logger.info("BookController: Email extracted from auth context: {}", email);

        // Verificar que el usuario existe
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        logger.info("BookController: User found: {} with role: {}", user.getEmail(), user.getRole());

        // Verificar que el usuario es bibliotecario
        if (user.getRole() != UserRole.LIBRARIAN) {
            logger.warn("BookController: User {} is not a librarian, role: {}", email, user.getRole());
            throw new UnauthorizedException(
                    "Acceso denegado: solo bibliotecarios pueden ver el catálogo de su biblioteca");

        }

        // Verificar que la biblioteca pertenece al usuario

        Librarian librarian = librarianRepository.findByUser(user)
                .orElseThrow(() -> new UnauthorizedException("Perfil de bibliotecario no encontrado"));
        logger.info("BookController: Librarian found for user: {}, libraryId: {}", email,
                librarian.getLibrary().getId());

        if (!librarian.getLibrary().getId().equals(libraryId)) {
            logger.warn("BookController: User {} trying to access library {} but belongs to library {}",
                    email, libraryId, librarian.getLibrary().getId());
            throw new UnauthorizedException(
                    "No tienes acceso a esta biblioteca. Solo puedes ver los libros de tu propia biblioteca.");
        }

        logger.info("BookController: All validations passed, returning books for library: {}", libraryId);
        // Si todo está bien, retornar los libros
        return ResponseEntity.ok(bookService.getBooksByLibrary(libraryId, page, size));

    }

    @GetMapping("/genres")

    public ResponseEntity<PaginatedResponse<BookSummaryDTO>> getBooksByGenres(
            @RequestParam List<Long> genreIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(bookService.getBooksByGenres(genreIds, page, size));

    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<PaginatedResponse<BookSummaryDTO>> getBooksByAuthor(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(bookService.getBooksByAuthor(authorId, page, size));

    }

    @GetMapping("/popular")
    public ResponseEntity<PaginatedResponse<BookSummaryDTO>> getMostPopular(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(bookService.getMostPopular(page, size));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<PaginatedResponse<BookSummaryDTO>> getTopRated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(bookService.getTopRated(page, size, genreId, year));
    }

    @GetMapping("/new-catalog")
    public ResponseEntity<PaginatedResponse<BookCatalogItemDTO>> getNewCatalogBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) String format) {
        return ResponseEntity.ok(bookService.getNewCatalogBooks(page, size, genreId, format));
    }

    @GetMapping("/featured")
    public ResponseEntity<FeaturedSectionsDTO> getFeaturedSections() {
        return ResponseEntity.ok(bookService.getFeaturedSections());
    }

    // ========== DETALLES ==========

    @GetMapping("/{id}")
    public ResponseEntity<BookDetailDTO> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<BookSummaryDTO>> getSimilarBooks(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getSimilarBooks(id));
    }

    @GetMapping("/{id}/share-link")
    public ResponseEntity<ShareLinkDTO> getBookShareLink(@PathVariable Long id) {
        String url = LinkGenerationUtil.generateBookShareLink(id);
        return ResponseEntity.ok(new ShareLinkDTO(url, "book"));
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDetailDTO> getBookByIsbn(@PathVariable Long isbn) {
        return ResponseEntity.ok(bookService.getBookByIsbn(isbn));
    }

    // ========== EDICIÓN Y ELIMINACIÓN ==========

    @DeleteMapping("/{id}/library")
    public ResponseEntity<String> removeBookFromLibrary(@PathVariable Long id) {

        // Obtener el usuario autenticado desde el contexto de seguridad de Spring
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("BookController: Remove book from library by user: {}", authentication.getName());

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("BookController: User is not authenticated");
            throw new UnauthorizedException("Usuario no autenticado");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("BookController: User not found with email: {}", email);
                    return new UnauthorizedException("Usuario no encontrado");
                });

        logger.info("BookController: Removing book {} from library for user: {}, role: {}", id, email, user.getRole());
        bookService.removeBookFromLibrary(id, user.getId());
        return ResponseEntity.ok("Libro eliminado de la biblioteca exitosamente.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable Long id) {

        // Obtener el usuario autenticado desde el contexto de seguridad de Spring
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("BookController: Delete book by user: {}", authentication.getName());

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("BookController: User is not authenticated");
            throw new UnauthorizedException("Usuario no autenticado");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("BookController: User not found with email: {}", email);
                    return new UnauthorizedException("Usuario no encontrado");
                });

        logger.info("BookController: Deleting book {} for user: {}, role: {}", id, email, user.getRole());
        bookService.deleteBook(id, user.getId());
        return ResponseEntity.ok("Libro eliminado del sistema exitosamente.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDetailDTO> updateBook(
            @PathVariable Long id,
            @RequestBody BookPublishRequestDTO requestDto) {

        // Obtener el usuario autenticado desde el contexto de seguridad de Spring
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("BookController: Update book by user: {}", authentication.getName());

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("BookController: User is not authenticated");
            throw new UnauthorizedException("Usuario no autenticado");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("BookController: User not found with email: {}", email);
                    return new UnauthorizedException("Usuario no encontrado");
                });

        logger.info("BookController: Updating book {} for user: {}, role: {}", id, email, user.getRole());
        BookDetailDTO updatedBook = bookService.updateBook(id, requestDto, user.getId());
        return ResponseEntity.ok(updatedBook);
    }

}

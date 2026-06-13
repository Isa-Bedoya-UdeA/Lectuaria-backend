package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.BookCatalogItemDTO;
import com.lectuaria.backend.dto.book.BookDetailDTO;
import com.lectuaria.backend.dto.book.BookFilterDTO;
import com.lectuaria.backend.dto.book.BookPublishRequestDTO;
import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.book.FeaturedSectionsDTO;
import com.lectuaria.backend.dto.common.PaginatedResponse;
import com.lectuaria.backend.dto.common.ShareLinkDTO;
import com.lectuaria.backend.exception.ForbiddenException;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.library.Librarian;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.book.IBookService;
import com.lectuaria.backend.util.BookResponseFactory;
import com.lectuaria.backend.util.HateoasLinkBuilder;
import com.lectuaria.backend.util.LinkGenerationUtil;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Endpoints REST para libros.
 * Las respuestas se sirven como recursos HATEOAS, lo que permite al cliente
 * descubrir hipermedia relacionada sin acoplarse a URLs hardcodeadas.
 *
 * Los enlaces que se exponen siguen el patron REST: cada recurso conoce
 * su self y las relaciones mas relevantes (similar, ratings, reviews,
 * share-link, remove-from-library). Las paginas se envuelven en
 * PagedModel con enlaces first/last/prev/next segun corresponda.
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final IBookService bookService;
    private final AuthenticatedUserResolver userResolver;
    private final LibrarianRepository librarianRepository;

    public BookController(IBookService bookService,
                          AuthenticatedUserResolver userResolver,
                          LibrarianRepository librarianRepository) {
        this.bookService = bookService;
        this.userResolver = userResolver;
        this.librarianRepository = librarianRepository;
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<BookSummaryDTO>>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Float minRating,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) List<String> formatTypes) {
        Long userId = userResolver.tryGetCurrentUserId();
        PaginatedResponse<BookSummaryDTO> response =
                bookService.getAllBooks(page, size, minRating, startYear, endYear, formatTypes, userId);
        return ResponseEntity.ok(HateoasLinkBuilder.wrapPage(response, BookController.class, page));
    }

    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<BookSummaryDTO>>> searchBooks(
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) List<Long> genreIds,
            @RequestParam(required = false) List<Long> libraryIds,
            @RequestParam(required = false) List<String> formatTypes,
            @RequestParam(required = false) Integer minYear,
            @RequestParam(required = false) Integer maxYear,
            @RequestParam(required = false) Float minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = userResolver.tryGetCurrentUserId();
        List<String> keywordList = null;
        if (keywords != null && !keywords.trim().isEmpty()) {
            keywordList = Arrays.stream(keywords.split("[,\\s]+"))
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .toList();
        }
        BookFilterDTO filter = new BookFilterDTO();
        filter.setKeywords(keywordList);
        filter.setGenreIds(genreIds);
        filter.setLibraryIds(libraryIds);
        filter.setFormatTypes(formatTypes);
        filter.setMinYear(minYear);
        filter.setMaxYear(maxYear);
        filter.setMinRating(minRating);
        PaginatedResponse<BookSummaryDTO> response =
                bookService.searchBooksByMultipleFilters(filter, page, size, userId);
        return ResponseEntity.ok(HateoasLinkBuilder.wrapPage(response, BookController.class, page));
    }

    @GetMapping("/genre/{genreId}")
    public ResponseEntity<PagedModel<EntityModel<BookSummaryDTO>>> getBooksByGenre(
            @PathVariable Long genreId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(HateoasLinkBuilder.wrapPage(
                bookService.getBooksByGenre(genreId, page, size), BookController.class, page));
    }

    @GetMapping("/filter/availability")
    public ResponseEntity<PagedModel<EntityModel<BookSummaryDTO>>> getBooksByFormatAvailability(
            @RequestParam String format,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = userResolver.tryGetCurrentUserId();
        return ResponseEntity.ok(HateoasLinkBuilder.wrapPage(
                bookService.getBooksByFormatAvailability(format, page, size, userId),
                BookController.class, page));
    }

    @GetMapping("/library/{libraryId}")
    public ResponseEntity<PagedModel<EntityModel<BookSummaryDTO>>> getBooksByLibrary(
            @PathVariable Long libraryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        if (user.getRole() != UserRole.LIBRARIAN) {
            throw new ForbiddenException("Acceso denegado: solo bibliotecarios pueden ver el catalogo de su biblioteca");
        }
        Librarian librarian = librarianRepository.findByUser(user)
                .orElseThrow(() -> new UnauthorizedException("Perfil de bibliotecario no encontrado"));
        if (!librarian.getLibrary().getId().equals(libraryId)) {
            throw new ForbiddenException("No tienes acceso a esta biblioteca.");
        }
        return ResponseEntity.ok(HateoasLinkBuilder.wrapPage(
                bookService.getBooksByLibrary(libraryId, page, size, keyword, sort),
                BookController.class, page));
    }

    @GetMapping("/genres")
    public ResponseEntity<PagedModel<EntityModel<BookSummaryDTO>>> getBooksByGenres(
            @RequestParam List<Long> genreIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(HateoasLinkBuilder.wrapPage(
                bookService.getBooksByGenres(genreIds, page, size), BookController.class, page));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<PagedModel<EntityModel<BookSummaryDTO>>> getBooksByAuthor(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(HateoasLinkBuilder.wrapPage(
                bookService.getBooksByAuthor(authorId, page, size), BookController.class, page));
    }

    @GetMapping("/popular")
    public ResponseEntity<PagedModel<EntityModel<BookSummaryDTO>>> getMostPopular(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(HateoasLinkBuilder.wrapPage(
                bookService.getMostPopular(page, size), BookController.class, page));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<PagedModel<EntityModel<BookSummaryDTO>>> getTopRated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(HateoasLinkBuilder.wrapPage(
                bookService.getTopRated(page, size, genreId, year), BookController.class, page));
    }

    @GetMapping("/new-catalog")
    public ResponseEntity<PagedModel<EntityModel<BookCatalogItemDTO>>> getNewCatalogBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long genreId) {
        return ResponseEntity.ok(HateoasLinkBuilder.wrapPage(
                bookService.getNewCatalogBooks(page, size, genreId), BookController.class, page));
    }

    @GetMapping("/featured")
    public ResponseEntity<EntityModel<FeaturedSectionsDTO>> getFeaturedSections() {
        return ResponseEntity.ok(EntityModel.of(bookService.getFeaturedSections(),
                linkTo(methodOn(BookController.class).getFeaturedSections()).withSelfRel(),
                linkTo(methodOn(BookController.class).getMostPopular(0, 12)).withRel("popular"),
                linkTo(methodOn(BookController.class).getTopRated(0, 12, null, null)).withRel("top-rated"),
                linkTo(methodOn(BookController.class).getNewCatalogBooks(0, 12, null)).withRel("new-catalog")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<BookDetailDTO>> getBookById(@PathVariable Long id) {
        BookDetailDTO book = bookService.getBookById(id);
        // Design Pattern: Factory Method (GoF) — BookResponseFactory centraliza
        // los enlaces hipermedia del recurso libro para que cualquier
        // endpoint que lo exponga use la misma semantica.
        return ResponseEntity.ok(BookResponseFactory.wrap(book));
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<CollectionModel<BookSummaryDTO>> getSimilarBooks(@PathVariable Long id) {
        return ResponseEntity.ok(CollectionModel.of(bookService.getSimilarBooks(id),
                linkTo(methodOn(BookController.class).getSimilarBooks(id)).withSelfRel(),
                linkTo(methodOn(BookController.class).getBookById(id)).withRel("book")));
    }

    @GetMapping("/{id}/share-link")
    public ResponseEntity<EntityModel<ShareLinkDTO>> getBookShareLink(@PathVariable Long id) {
        String url = LinkGenerationUtil.generateBookShareLink(id);
        ShareLinkDTO payload = new ShareLinkDTO(url, "book");
        return ResponseEntity.ok(EntityModel.of(payload,
                linkTo(methodOn(BookController.class).getBookShareLink(id)).withSelfRel(),
                linkTo(methodOn(BookController.class).getBookById(id)).withRel("book")));
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<EntityModel<BookDetailDTO>> getBookByIsbn(@PathVariable Long isbn) {
        BookDetailDTO book = bookService.getBookByIsbn(isbn);
        return ResponseEntity.ok(EntityModel.of(book,
                linkTo(methodOn(BookController.class).getBookByIsbn(isbn)).withSelfRel(),
                linkTo(methodOn(BookController.class).getBookById(book.getId())).withRel("by-id")));
    }

    @DeleteMapping("/{id}/library")
    public ResponseEntity<String> removeBookFromLibrary(@PathVariable Long id,
                                                       HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        bookService.removeBookFromLibrary(id, user.getId());
        // Antes retornabamos EntityModel<String> con un Link HATEOAS, pero
        // Jackson no puede serializar EntityModel cuyo content es un String
        // escalar junto con un _links (lanza "Can not write a string, expecting
        // field name"). Devolvemos un String plano: el body es el mensaje y
        // los clientes que necesiten el link al recurso pueden llamar a
        // GET /api/books/{id}. Mantiene el contrato del front (espera un
        // body string) sin romper la serializacion.
        return ResponseEntity.ok("Libro eliminado de la biblioteca exitosamente.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable Long id,
                                              HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        bookService.deleteBook(id, user.getId());
        // Mismo fix que removeBookFromLibrary: EntityModel<String> con Links
        // rompe la serializacion de Jackson. Devolvemos String plano.
        return ResponseEntity.ok("Libro eliminado del sistema exitosamente.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<BookDetailDTO>> updateBook(
            @PathVariable Long id,
            @RequestBody BookPublishRequestDTO requestDto,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        BookDetailDTO updatedBook = bookService.updateBook(id, requestDto, user.getId());
        return ResponseEntity.ok(EntityModel.of(updatedBook,
                linkTo(methodOn(BookController.class).getBookById(id)).withSelfRel()));
    }
}

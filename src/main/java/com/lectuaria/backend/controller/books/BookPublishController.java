package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.BookPublishRequestDTO;
import com.lectuaria.backend.dto.book.BookPublishResponseDTO;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.book.IBookPublishService;
import com.lectuaria.backend.service.storage.S3StorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;


@RestController
@RequestMapping("/api/books")
public class BookPublishController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookPublishController.class);
    private final IBookPublishService bookPublishService;
    private final S3StorageService s3StorageService;
    private final AuthenticatedUserResolver userResolver;

    public BookPublishController(IBookPublishService bookPublishService,
                                  S3StorageService s3StorageService,
                                  AuthenticatedUserResolver userResolver) {
        this.bookPublishService = bookPublishService;
        this.s3StorageService = s3StorageService;
        this.userResolver = userResolver;
    }

    @PostMapping("/publish")
    public ResponseEntity<EntityModel<BookPublishResponseDTO>> publishBook(
            @Valid @RequestBody BookPublishRequestDTO request,
            HttpServletRequest httpRequest) {
        Long librarianUserId = userResolver.requireCurrentUser(httpRequest).getId();
        BookPublishResponseDTO body = bookPublishService.publishBook(request, librarianUserId);
        EntityModel<BookPublishResponseDTO> model = EntityModel.of(body);
        model.add(linkTo(methodOn(BookController.class).getBookById(body.getBookId())).withRel("book"));
        return ResponseEntity.ok(model);
    }

    @PostMapping("/publish-with-cover")
    public ResponseEntity<EntityModel<BookPublishResponseDTO>> publishBookWithCover(
            @RequestBody BookPublishRequestDTO request,
            HttpServletRequest httpRequest) throws Exception {
        Long librarianUserId = userResolver.requireCurrentUser(httpRequest).getId();

        // If cover image provided as base64, upload to S3 and set as coverUrl
        if (request.getCoverUrl() != null && request.getCoverUrl().startsWith("data:")) {
            String dataUri = request.getCoverUrl();
            String mimeType = dataUri.substring("data:".length(), dataUri.indexOf(";"));
            String base64Data = dataUri.substring(dataUri.indexOf(",") + 1);
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
            long isbn = request.getIsbn();
            String coverUrl = s3StorageService.uploadCoverBytes(imageBytes, isbn, mimeType);
            request.setCoverUrl(coverUrl);
            LOGGER.info("Cover image uploaded for ISBN {}: {}", isbn, coverUrl);
        }

        BookPublishResponseDTO body = bookPublishService.publishBook(request, librarianUserId);
        EntityModel<BookPublishResponseDTO> model = EntityModel.of(body);
        model.add(linkTo(methodOn(BookController.class).getBookById(body.getBookId())).withRel("book"));
        return ResponseEntity.ok(model);
    }

    @GetMapping("/prefill/{isbn}")
    public ResponseEntity<EntityModel<BookPublishRequestDTO>> prefillFromOpenLibrary(@PathVariable @NonNull Long isbn,
                                                                                     HttpServletRequest httpRequest) {
        Long librarianUserId = userResolver.requireCurrentUser(httpRequest).getId();
        BookPublishRequestDTO body = bookPublishService.prefillFromOpenLibrary(isbn, librarianUserId);

        LOGGER.info("Controller - Response data: Title={}, Authors={}, Description={}, CoverUrl={}, Publishers={}",
                body.getTitle(),
                body.getAuthors(),
                body.getDescription(),
                body.getCoverUrl(),
                body.getPublishers());

        EntityModel<BookPublishRequestDTO> model = EntityModel.of(body);
        model.add(linkTo(methodOn(BookPublishController.class).prefillFromOpenLibrary(isbn, null))
                .withSelfRel());
        return ResponseEntity.ok(model);
    }
}

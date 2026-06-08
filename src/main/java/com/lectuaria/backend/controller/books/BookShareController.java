package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.BookShareRequestDTO;
import com.lectuaria.backend.dto.book.BookShareResponseDTO;
import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.book.IBookShareService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/books")
public class BookShareController {

    private final IBookShareService bookShareService;
    private final AuthenticatedUserResolver userResolver;

    public BookShareController(IBookShareService bookShareService,
                                AuthenticatedUserResolver userResolver) {
        this.bookShareService = bookShareService;
        this.userResolver = userResolver;
    }

    @PostMapping("/{bookId}/share")
    public ResponseEntity<EntityModel<ShareResultDTO>> shareBook(
            @PathVariable Long bookId,
            @RequestBody BookShareRequestDTO request,
            HttpServletRequest httpRequest) {
        User sender = userResolver.requireCurrentUser(httpRequest);
        ShareResultDTO result = bookShareService.shareBookWithFriends(bookId, request, sender);
        return ResponseEntity.ok(EntityModel.of(result,
                linkTo(methodOn(BookShareController.class).shareBook(bookId, request, httpRequest)).withSelfRel(),
                linkTo(methodOn(BookShareController.class).getReceivedShares(httpRequest)).withRel("received"),
                linkTo(methodOn(BookShareController.class).getSentShares(httpRequest)).withRel("sent")));
    }

    @GetMapping("/shares/received")
    public ResponseEntity<CollectionModel<BookShareResponseDTO>> getReceivedShares(HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        return ResponseEntity.ok(CollectionModel.of(bookShareService.getReceivedShares(user),
                linkTo(methodOn(BookShareController.class).getReceivedShares(request)).withSelfRel(),
                linkTo(methodOn(BookShareController.class).getSentShares(request)).withRel("sent")));
    }

    @GetMapping("/shares/sent")
    public ResponseEntity<CollectionModel<BookShareResponseDTO>> getSentShares(HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        return ResponseEntity.ok(CollectionModel.of(bookShareService.getSentShares(user),
                linkTo(methodOn(BookShareController.class).getSentShares(request)).withSelfRel(),
                linkTo(methodOn(BookShareController.class).getReceivedShares(request)).withRel("received")));
    }

    @GetMapping("/{bookId}/shared-with/{friendId}")
    public ResponseEntity<EntityModel<java.util.Map<String, Boolean>>> isBookSharedWithFriend(
            @PathVariable Long bookId,
            @PathVariable Long friendId,
            HttpServletRequest request) {
        User sender = userResolver.requireCurrentUser(request);
        boolean isShared = bookShareService.isBookSharedWithFriend(sender.getId(), friendId, bookId);
        java.util.Map<String, Boolean> body = java.util.Collections.singletonMap("isShared", isShared);
        return ResponseEntity.ok(EntityModel.of(body,
                linkTo(methodOn(BookShareController.class).isBookSharedWithFriend(bookId, friendId, request)).withSelfRel()));
    }
}

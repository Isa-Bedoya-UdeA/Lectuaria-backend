package com.lectuaria.backend.controller.friendship;

import com.lectuaria.backend.dto.common.UserSearchResponseDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.friendship.IFriendshipService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/friendships")
public class FriendshipController {

    private final IFriendshipService friendshipService;
    private final AuthenticatedUserResolver userResolver;

    public FriendshipController(IFriendshipService friendshipService,
                               AuthenticatedUserResolver userResolver) {
        this.friendshipService = friendshipService;
        this.userResolver = userResolver;
    }

    @GetMapping("/search")
    public ResponseEntity<CollectionModel<UserSearchResponseDTO>> searchReaders(
            @RequestParam String query,
            HttpServletRequest request) {
        User user = userResolver.tryGetCurrentUser(request);
        requireNotLibrarian(user);
        List<UserSearchResponseDTO> results = friendshipService.searchReaders(query, user);
        return ResponseEntity.ok(CollectionModel.of(results,
                linkTo(methodOn(FriendshipController.class).searchReaders(query, request)).withSelfRel(),
                linkTo(methodOn(FriendshipController.class).sendFriendshipRequest(0L, request)).withRel("send-request")));
    }

    @GetMapping
    public ResponseEntity<CollectionModel<UserSearchResponseDTO>> getFriends(HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireNotLibrarian(user);
        List<UserSearchResponseDTO> friends = friendshipService.getFriends(user);
        return ResponseEntity.ok(CollectionModel.of(friends,
                linkTo(methodOn(FriendshipController.class).getFriends(request)).withSelfRel(),
                linkTo(methodOn(FriendshipController.class).getPendingRequests(request)).withRel("pending-requests")));
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<CollectionModel<UserSearchResponseDTO>> getPendingRequests(HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireNotLibrarian(user);
        List<UserSearchResponseDTO> pending = friendshipService.getPendingRequests(user);
        return ResponseEntity.ok(CollectionModel.of(pending,
                linkTo(methodOn(FriendshipController.class).getPendingRequests(request)).withSelfRel()));
    }

    @PostMapping("/requests/{receiverId}")
    public ResponseEntity<Void> sendFriendshipRequest(
            @PathVariable Long receiverId,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireNotLibrarian(user);
        friendshipService.sendFriendshipRequest(receiverId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<Void> acceptFriendshipRequest(
            @PathVariable Long requestId,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        friendshipService.acceptFriendshipRequest(requestId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<Void> rejectFriendshipRequest(
            @PathVariable Long requestId,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        friendshipService.rejectFriendshipRequest(requestId, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> cancelFriendshipRequest(
            @PathVariable Long requestId,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        friendshipService.cancelFriendshipRequest(requestId, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriendship(
            @PathVariable Long friendId,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireNotLibrarian(user);
        friendshipService.removeFriendship(friendId, user);
        return ResponseEntity.ok().build();
    }

    private void requireNotLibrarian(User user) {
        if (user != null && user.getRole() == UserRole.LIBRARIAN) {
            throw new UnauthorizedException("Los bibliotecarios no tienen acceso a opciones de amistad");
        }
    }
}

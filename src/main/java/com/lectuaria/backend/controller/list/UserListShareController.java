package com.lectuaria.backend.controller.list;

import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;
import com.lectuaria.backend.dto.list.UserListShareMultipleDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.list.IUserListShareService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Expone las operaciones de compartición de listas de usuario.
 * Cubre los endpoints que consume el cliente en listShareService.ts:
 *   - POST /api/user-list-shares/{listId}/share-multiple
 *   - POST /api/user-list-shares/{listId}/share/{friendId}
 *   - DELETE /api/user-list-shares/{shareId}
 *   - GET  /api/user-list-shares/public/{token}   (permitAll en SecurityConfig)
 */
@RestController
@RequestMapping("/api/user-list-shares")
public class UserListShareController {

    private final IUserListShareService userListShareService;
    private final AuthenticatedUserResolver userResolver;

    public UserListShareController(IUserListShareService userListShareService,
                                   AuthenticatedUserResolver userResolver) {
        this.userListShareService = userListShareService;
        this.userResolver = userResolver;
    }

    /**
     * Comparte una lista con varios amigos en una sola llamada.
     * Mapea contra {@code POST /user-list-shares/{listId}/share-multiple}.
     * Si la lista es PRIVADA lanza {@link com.lectuaria.backend.exception.list.PrivateListException}
     * (HTTP 400) gestionada por el GlobalExceptionHandler.
     */
    @PostMapping("/{listId}/share-multiple")
    public ResponseEntity<EntityModel<ShareResultDTO>> shareWithMultiple(
            @PathVariable Long listId,
            @RequestBody UserListShareMultipleDTO request,
            HttpServletRequest httpRequest) {
        User owner = userResolver.requireCurrentUser(httpRequest);
        ShareResultDTO result = userListShareService.shareListWithMultipleFriends(
                listId, request.getFriendIds(), request.getMessage(), owner.getId());
        return ResponseEntity.ok(EntityModel.of(result,
                linkTo(methodOn(UserListShareController.class)
                        .shareWithMultiple(listId, request, httpRequest)).withSelfRel()));
    }

    /**
     * Comparte una lista con un solo amigo.
     * Mapea contra {@code POST /user-list-shares/{listId}/share/{friendId}}.
     */
    @PostMapping("/{listId}/share/{friendId}")
    public ResponseEntity<EntityModel<UserListShareDTO>> shareWithSingleFriend(
            @PathVariable Long listId,
            @PathVariable Long friendId,
            HttpServletRequest httpRequest) {
        User owner = userResolver.requireCurrentUser(httpRequest);
        UserListShareDTO result = userListShareService.shareListWithFriends(
                listId, List.of(friendId), owner.getId());
        return ResponseEntity.ok(EntityModel.of(result,
                linkTo(methodOn(UserListShareController.class)
                        .shareWithSingleFriend(listId, friendId, httpRequest)).withSelfRel()));
    }

    /**
     * Revoca un share (lo marca como inactivo).
     * Mapea contra {@code DELETE /user-list-shares/{shareId}}.
     */
    @DeleteMapping("/{shareId}")
    public ResponseEntity<EntityModel<Map<String, String>>> revokeShare(
            @PathVariable Long shareId,
            HttpServletRequest httpRequest) {
        User owner = userResolver.requireCurrentUser(httpRequest);
        userListShareService.revokeShare(shareId, owner.getId());
        return ResponseEntity.ok(EntityModel.of(Map.of("message", "Compartición revocada correctamente"),
                linkTo(methodOn(UserListShareController.class)
                        .revokeShare(shareId, httpRequest)).withSelfRel()));
    }

    /**
     * Resuelve un share por su token público (visibilidad LISTED).
     * Endpoint público (permitAll en SecurityConfig).
     * Mapea contra {@code GET /user-list-shares/public/{token}}.
     */
    @GetMapping("/public/{token}")
    public ResponseEntity<EntityModel<UserListShareDTO>> getByPublicToken(@PathVariable String token) {
        UserListShareDTO result = userListShareService.getListByPublicToken(token);
        return ResponseEntity.ok(EntityModel.of(result,
                linkTo(methodOn(UserListShareController.class)
                        .getByPublicToken(token)).withSelfRel()));
    }
}

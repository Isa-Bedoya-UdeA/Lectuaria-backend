package com.lectuaria.backend.controller.list;

import com.lectuaria.backend.dto.list.CreateListRequestDTO;
import com.lectuaria.backend.dto.list.FavoriteToggleResponseDTO;
import com.lectuaria.backend.dto.list.MoveBookResponseDTO;
import com.lectuaria.backend.dto.list.UserListDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.list.IUserListService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/lists")
public class UserListController {

    private final IUserListService listService;
    private final AuthenticatedUserResolver userResolver;

    public UserListController(IUserListService listService, AuthenticatedUserResolver userResolver) {
        this.listService = listService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<CollectionModel<UserListDTO>> getMyLists(HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireReaderRole(user);
        List<UserListDTO> lists = listService.getUserLists(user.getId());
        return ResponseEntity.ok(CollectionModel.of(lists,
                linkTo(methodOn(UserListController.class).getMyLists(request)).withSelfRel(),
                linkTo(methodOn(UserListController.class).createList(null, request)).withRel("create"),
                linkTo(methodOn(UserListController.class).getFavorites(request)).withRel("favorites")));
    }

    @GetMapping("/{listId}")
    public ResponseEntity<EntityModel<UserListDTO>> getListDetails(@PathVariable Long listId,
                                                                   HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireReaderRole(user);
        UserListDTO list = listService.getListDetails(listId, user.getId());
        return ResponseEntity.ok(EntityModel.of(list,
                linkTo(methodOn(UserListController.class).getListDetails(listId, request)).withSelfRel(),
                linkTo(methodOn(UserListController.class).addBook(listId, null, false, request)).withRel("add-book"),
                linkTo(methodOn(UserListController.class).deleteList(listId, false, false, request)).withRel("delete")));
    }

    @PostMapping
    public ResponseEntity<EntityModel<UserListDTO>> createList(
            @RequestBody CreateListRequestDTO requestDto,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireReaderRole(user);
        UserListDTO created = listService.createCustomList(requestDto, user);
        return ResponseEntity.ok(EntityModel.of(created,
                linkTo(methodOn(UserListController.class).getListDetails(created.getId(), request)).withSelfRel()));
    }

    @PostMapping("/{listId}/books/{bookId}")
    public ResponseEntity<Void> addBook(
            @PathVariable Long listId,
            @PathVariable Long bookId,
            @RequestParam(required = false, defaultValue = "false") boolean force,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireReaderRole(user);
        listService.addBookToList(listId, bookId, user, force);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{sourceListId}/books/{bookId}/move/{targetListId}")
    public ResponseEntity<EntityModel<MoveBookResponseDTO>> moveBook(
            @PathVariable Long sourceListId,
            @PathVariable Long bookId,
            @PathVariable Long targetListId,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireReaderRole(user);
        MoveBookResponseDTO response = listService.moveBookBetweenLists(sourceListId, targetListId, bookId, user);
        return ResponseEntity.ok(EntityModel.of(response,
                linkTo(methodOn(UserListController.class).getListDetails(sourceListId, request)).withRel("source"),
                linkTo(methodOn(UserListController.class).getListDetails(targetListId, request)).withRel("target")));
    }

    @DeleteMapping("/{listId}/books/{bookId}")
    public ResponseEntity<Void> removeBook(
            @PathVariable Long listId,
            @PathVariable Long bookId,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireReaderRole(user);
        listService.removeBookFromList(listId, bookId, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(
            @PathVariable Long listId,
            @RequestParam(defaultValue = "false") boolean confirm,
            @RequestParam(defaultValue = "false") boolean force,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireReaderRole(user);
        listService.deleteList(listId, user, confirm, force);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/favorites/books/{bookId}/toggle")
    public ResponseEntity<EntityModel<FavoriteToggleResponseDTO>> toggleFavorite(
            @PathVariable Long bookId,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireReaderRole(user);
        boolean isFavorite = listService.toggleFavorite(bookId, user);
        FavoriteToggleResponseDTO payload = new FavoriteToggleResponseDTO(bookId, isFavorite);
        return ResponseEntity.ok(EntityModel.of(payload,
                linkTo(methodOn(UserListController.class).getFavorites(request)).withRel("favorites")));
    }

    @GetMapping("/favorites")
    public ResponseEntity<EntityModel<UserListDTO>> getFavorites(HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        requireReaderRole(user);
        UserListDTO favorites = listService.getMyFavorites(user);
        return ResponseEntity.ok(EntityModel.of(favorites,
                linkTo(methodOn(UserListController.class).getFavorites(request)).withSelfRel(),
                linkTo(methodOn(UserListController.class).toggleFavorite(0L, request)).withRel("toggle")));
    }

    private void requireReaderRole(User user) {
        if (user.getRole() == UserRole.LIBRARIAN) {
            throw new UnauthorizedException("Los bibliotecarios no pueden tener listas de lectura.");
        }
    }
}

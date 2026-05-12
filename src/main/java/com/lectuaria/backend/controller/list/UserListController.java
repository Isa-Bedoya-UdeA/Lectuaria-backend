package com.lectuaria.backend.controller.list;

import com.lectuaria.backend.dto.list.CreateListRequestDTO;
import com.lectuaria.backend.dto.list.FavoriteToggleResponseDTO;
import com.lectuaria.backend.dto.list.UserListDTO;
import com.lectuaria.backend.dto.list.MoveBookResponseDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.list.IUserListService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
public class UserListController {

    private final IUserListService listService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public UserListController(IUserListService listService, JwtService jwtService, UserRepository userRepository) {
        this.listService = listService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<UserListDTO>> getMyLists(HttpServletRequest request) {
        User user = extractUser(request);
        checkRole(user);
        return ResponseEntity.ok(listService.getUserLists(user.getId()));
    }

    @GetMapping("/{listId}")
    public ResponseEntity<UserListDTO> getListDetails(@PathVariable Long listId, HttpServletRequest request) {
        User user = extractUser(request);
        checkRole(user);
        return ResponseEntity.ok(listService.getListDetails(listId, user.getId()));
    }

    @PostMapping
    public ResponseEntity<UserListDTO> createList(
            @RequestBody CreateListRequestDTO requestDto,
            HttpServletRequest request) {
        User user = extractUser(request);
        checkRole(user);
        return ResponseEntity.ok(listService.createCustomList(requestDto, user));
    }

    @PostMapping("/{listId}/books/{bookId}")
    public ResponseEntity<Void> addBook(
            @PathVariable Long listId,
            @PathVariable Long bookId,
            @RequestParam(required = false, defaultValue = "false") boolean force,
            HttpServletRequest request) {
        User user = extractUser(request);
        checkRole(user);
        listService.addBookToList(listId, bookId, user, force);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{sourceListId}/books/{bookId}/move/{targetListId}")
    public ResponseEntity<MoveBookResponseDTO> moveBook(
            @PathVariable Long sourceListId,
            @PathVariable Long bookId,
            @PathVariable Long targetListId,
            HttpServletRequest request) {
        User user = extractUser(request);
        checkRole(user);
        return ResponseEntity.ok(listService.moveBookBetweenLists(sourceListId, targetListId, bookId, user));
    }

    @DeleteMapping("/{listId}/books/{bookId}")
    public ResponseEntity<Void> removeBook(
            @PathVariable Long listId,
            @PathVariable Long bookId,
            HttpServletRequest request) {
        User user = extractUser(request);
        checkRole(user);
        listService.removeBookFromList(listId, bookId, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(
            @PathVariable Long listId,
            @RequestParam(defaultValue = "false") boolean confirm,
            @RequestParam(defaultValue = "false") boolean force,
            HttpServletRequest request) {
        User user = extractUser(request);
        checkRole(user);
        listService.deleteList(listId, user, confirm, force);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/favorites/books/{bookId}/toggle")
    public ResponseEntity<FavoriteToggleResponseDTO> toggleFavorite(
            @PathVariable Long bookId,
            HttpServletRequest request) {
        User user = extractUser(request);
        checkRole(user);
        boolean isFavorite = listService.toggleFavorite(bookId, user);
        return ResponseEntity.ok(new FavoriteToggleResponseDTO(bookId, isFavorite));
    }

    @GetMapping("/favorites")
    public ResponseEntity<UserListDTO> getFavorites(HttpServletRequest request) {
        User user = extractUser(request);
        checkRole(user);
        return ResponseEntity.ok(listService.getMyFavorites(user));
    }

    private User extractUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Token de autorización requerido");
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    private void checkRole(User user) {
        if (user.getRole() == UserRole.LIBRARIAN) {
            throw new UnauthorizedException("Los bibliotecarios no pueden tener listas de lectura.");
        }
    }
}

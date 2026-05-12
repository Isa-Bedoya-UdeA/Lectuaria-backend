package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.BookShareRequestDTO;
import com.lectuaria.backend.dto.book.BookShareResponseDTO;
import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.book.IBookShareService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookShareController {

    private final IBookShareService bookShareService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public BookShareController(IBookShareService bookShareService,
                               JwtService jwtService,
                               UserRepository userRepository) {
        this.bookShareService = bookShareService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{bookId}/share")
    public ResponseEntity<ShareResultDTO> shareBook(
            @PathVariable Long bookId,
            @RequestBody BookShareRequestDTO request,
            HttpServletRequest httpRequest) {
        User sender = extractUser(httpRequest);
        ShareResultDTO result = bookShareService.shareBookWithFriends(bookId, request, sender);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/shares/received")
    public ResponseEntity<List<BookShareResponseDTO>> getReceivedShares(HttpServletRequest request) {
        User user = extractUser(request);
        return ResponseEntity.ok(bookShareService.getReceivedShares(user));
    }

    @GetMapping("/shares/sent")
    public ResponseEntity<List<BookShareResponseDTO>> getSentShares(HttpServletRequest request) {
        User user = extractUser(request);
        return ResponseEntity.ok(bookShareService.getSentShares(user));
    }

    @GetMapping("/{bookId}/shared-with/{friendId}")
    public ResponseEntity<Boolean> isBookSharedWithFriend(
            @PathVariable Long bookId,
            @PathVariable Long friendId,
            HttpServletRequest request) {
        User sender = extractUser(request);
        boolean isShared = bookShareService.isBookSharedWithFriend(sender.getId(), friendId, bookId);
        return ResponseEntity.ok(isShared);
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
}

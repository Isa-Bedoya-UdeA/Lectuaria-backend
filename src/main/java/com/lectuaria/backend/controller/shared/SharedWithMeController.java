package com.lectuaria.backend.controller.shared;

import com.lectuaria.backend.dto.shared.SharedBookDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.service.shared.ISharedWithMeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shared-with-me")
public class SharedWithMeController {

    private final ISharedWithMeService sharedWithMeService;
    private final UserRepository userRepository;

    public SharedWithMeController(ISharedWithMeService sharedWithMeService, UserRepository userRepository) {
        this.sharedWithMeService = sharedWithMeService;
        this.userRepository = userRepository;
    }

    @GetMapping("/lists")
    public ResponseEntity<List<UserListShareDTO>> getSharedLists() {
        Long userId = getCurrentUserId();
        List<UserListShareDTO> sharedLists = sharedWithMeService.getSharedLists(userId);
        return ResponseEntity.ok(sharedLists);
    }

    @GetMapping("/books")
    public ResponseEntity<List<SharedBookDTO>> getSharedBooks() {
        Long userId = getCurrentUserId();
        List<SharedBookDTO> sharedBooks = sharedWithMeService.getSharedBooks(userId);
        return ResponseEntity.ok(sharedBooks);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                return user.getId();
            }
        }
        return null;
    }
}

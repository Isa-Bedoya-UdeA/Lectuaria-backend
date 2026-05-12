package com.lectuaria.backend.controller.list;

import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;
import com.lectuaria.backend.dto.list.UserListShareLinkDTO;
import com.lectuaria.backend.dto.list.UserListShareMultipleDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.list.IUserListShareService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-list-shares")
public class UserListShareController {

    private final IUserListShareService shareService;
    private final UserRepository userRepository;

    public UserListShareController(IUserListShareService shareService, UserRepository userRepository) {
        this.shareService = shareService;
        this.userRepository = userRepository;
    }

    @PostMapping("/share/{listId}")
    public ResponseEntity<UserListShareDTO> shareWithFriends(
            @PathVariable Long listId,
            @RequestBody List<Long> friendIds) {
        Long ownerId = getCurrentUserId();
        UserListShareDTO share = shareService.shareListWithFriends(listId, friendIds, ownerId);
        return ResponseEntity.ok(share);
    }

    @PostMapping("/{listId}/share-multiple")
    public ResponseEntity<ShareResultDTO> shareListWithMultipleFriends(
            @PathVariable Long listId,
            @RequestBody UserListShareMultipleDTO request) {
        Long ownerId = getCurrentUserId();
        ShareResultDTO result = shareService.shareListWithMultipleFriends(listId, request.getFriendIds(), request.getMessage(), ownerId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/public-link/{listId}")
    public ResponseEntity<UserListShareLinkDTO> generatePublicLink(@PathVariable Long listId) {
        Long ownerId = getCurrentUserId();
        UserListShareLinkDTO link = shareService.generatePublicLink(listId, ownerId);
        return ResponseEntity.ok(link);
    }

    @DeleteMapping("/revoke/{shareId}")
    public ResponseEntity<Void> revokeShare(@PathVariable Long shareId) {
        Long ownerId = getCurrentUserId();
        shareService.revokeShare(shareId, ownerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/public-link/{linkId}")
    public ResponseEntity<Void> revokePublicLink(@PathVariable Long linkId) {
        Long ownerId = getCurrentUserId();
        shareService.revokePublicLink(linkId, ownerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/received")
    public ResponseEntity<List<UserListShareDTO>> getSharedLists() {
        Long userId = getCurrentUserId();
        List<UserListShareDTO> shares = shareService.getSharedLists(userId);
        return ResponseEntity.ok(shares);
    }

    @GetMapping("/public/{token}")
    public ResponseEntity<UserListShareDTO> getListByPublicToken(@PathVariable String token) {
        UserListShareDTO share = shareService.getListByPublicToken(token);
        return ResponseEntity.ok(share);
    }

    @GetMapping("/public-link/{listId}")
    public ResponseEntity<List<UserListShareLinkDTO>> getPublicLinks(@PathVariable Long listId) {
        Long ownerId = getCurrentUserId();
        List<UserListShareLinkDTO> links = shareService.getPublicLinks(listId, ownerId);
        return ResponseEntity.ok(links);
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

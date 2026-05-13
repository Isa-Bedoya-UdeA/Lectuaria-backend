package com.lectuaria.backend.controller.user;

import com.lectuaria.backend.dto.user.FriendActivityDTO;
import com.lectuaria.backend.dto.user.UserProfileDTO;
import com.lectuaria.backend.dto.user.UserStatsDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.user.IUserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final IUserProfileService userProfileService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public UserProfileController(IUserProfileService userProfileService,
                                  JwtService jwtService,
                                  UserRepository userRepository) {
        this.userProfileService = userProfileService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{usernameSlug}")
    public ResponseEntity<UserProfileDTO> getUserProfile(
            @PathVariable String usernameSlug,
            HttpServletRequest request) {
        User currentUser = tryExtractUser(request);
        return ResponseEntity.ok(userProfileService.getUserProfileByUsername(usernameSlug, currentUser));
    }

    @GetMapping("/{usernameSlug}/stats")
    public ResponseEntity<UserStatsDTO> getUserStats(@PathVariable String usernameSlug) {
        return ResponseEntity.ok(userProfileService.getUserStats(usernameSlug));
    }

    @GetMapping("/{usernameSlug}/activity")
    public ResponseEntity<List<FriendActivityDTO>> getFriendActivity(
            @PathVariable String usernameSlug,
            HttpServletRequest request) {
        User currentUser = tryExtractUser(request);
        List<FriendActivityDTO> activities = userProfileService.getFriendActivity(usernameSlug, currentUser);
        return ResponseEntity.ok(activities);
    }

    private User tryExtractUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}

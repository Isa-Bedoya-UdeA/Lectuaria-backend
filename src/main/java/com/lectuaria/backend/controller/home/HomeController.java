package com.lectuaria.backend.controller.home;

import com.lectuaria.backend.dto.home.FriendActivityDTO;
import com.lectuaria.backend.dto.home.HomeResponseDTO;
import com.lectuaria.backend.dto.recommendation.RecommendationDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.home.IHomeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
public class HomeController {
    private final IHomeService homeService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public HomeController(IHomeService homeService, JwtService jwtService, UserRepository userRepository) {
        this.homeService = homeService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<HomeResponseDTO> getHome(
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) String format,
            HttpServletRequest request) {
        return ResponseEntity.ok(homeService.getHome(extractUser(request), genreId, format));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        return ResponseEntity.ok(homeService.getRecommendations(extractUser(request), size));
    }

    @DeleteMapping("/recommendations/{bookId}")
    public ResponseEntity<Void> hideRecommendation(
            @PathVariable Long bookId,
            HttpServletRequest request) {
        homeService.hideRecommendation(extractUser(request), bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/friends/activity")
    public ResponseEntity<List<FriendActivityDTO>> getFriendActivity(
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return ResponseEntity.ok(homeService.getFriendActivity(extractUser(request), size));
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

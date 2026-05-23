package com.lectuaria.backend.controller.home;

import com.lectuaria.backend.dto.book.FeaturedSectionsDTO;
import com.lectuaria.backend.dto.home.FriendActivityDTO;
import com.lectuaria.backend.dto.home.HomeResponseDTO;
import com.lectuaria.backend.dto.recommendation.RecommendationDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.home.IHomeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IHomeService homeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getHome_returnsHomeData() throws Exception {
        String token = "valid-token";
        User user = new User("Test User", "test@example.com", "hash", UserRole.READER, "testuser", null, null);

        when(jwtService.extractEmail(token)).thenReturn("test@example.com");
        when(jwtService.isTokenValid(token, "test@example.com")).thenReturn(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(homeService.getHome(any(), any())).thenReturn(
                new HomeResponseDTO(
                        java.util.List.of(),
                        java.util.List.of(),
                        new FeaturedSectionsDTO(java.util.List.of(), java.util.List.of(), Instant.now()),
                        java.util.List.of()
                )
        );

        mockMvc.perform(get("/api/home")
                        .header("Authorization", "Bearer " + token)
                        .param("genreId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendActivity").isArray())
                .andExpect(jsonPath("$.newCatalogBooks").isArray())
                .andExpect(jsonPath("$.featuredSections").exists())
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void getHome_rejectsWithoutAuthHeader() throws Exception {
        mockMvc.perform(get("/api/home"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getHome_rejectsWithInvalidToken() throws Exception {
        when(jwtService.extractEmail("bad-token")).thenThrow(
                new com.lectuaria.backend.exception.UnauthorizedException("Token inválido"));

        mockMvc.perform(get("/api/home")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRecommendations_returnsRecommendations() throws Exception {
        String token = "valid-token";
        User user = new User("Test User", "test@example.com", "hash", UserRole.READER, "testuser", null, null);

        when(jwtService.extractEmail(token)).thenReturn("test@example.com");
        when(jwtService.isTokenValid(token, "test@example.com")).thenReturn(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(homeService.getRecommendations(any(), any(Integer.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/home/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getFriendActivity_returnsActivityList() throws Exception {
        String token = "valid-token";
        User user = new User("Test User", "test@example.com", "hash", UserRole.READER, "testuser", null, null);

        when(jwtService.extractEmail(token)).thenReturn("test@example.com");
        when(jwtService.isTokenValid(token, "test@example.com")).thenReturn(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(homeService.getFriendActivity(any(), any(Integer.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/home/friends/activity")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void hideRecommendation_returns204() throws Exception {
        String token = "valid-token";
        User user = new User("Test User", "test@example.com", "hash", UserRole.READER, "testuser", null, null);

        when(jwtService.extractEmail(token)).thenReturn("test@example.com");
        when(jwtService.isTokenValid(token, "test@example.com")).thenReturn(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        doNothing().when(homeService).hideRecommendation(any(), any(Long.class));

        mockMvc.perform(delete("/api/home/recommendations/50")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
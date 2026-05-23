package com.lectuaria.backend.controller;

import com.lectuaria.backend.dto.auth.LoginRequestDTO;
import com.lectuaria.backend.dto.auth.LoginResponseDTO;
import com.lectuaria.backend.dto.auth.ProfileResponseDTO;
import com.lectuaria.backend.dto.auth.ProfileUpdateRequestDTO;
import com.lectuaria.backend.dto.auth.RegisterRequestDTO;
import com.lectuaria.backend.dto.auth.RegisterResponseDTO;
import java.util.Objects;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.exception.ValidationException;
import com.lectuaria.backend.service.auth.IAuthService;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.repository.auth.UserRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private IAuthService authService;

  @MockBean
  private JwtService jwtService;

  @MockBean
  private UserRepository userRepository;

  private static final String JSON_PATH_ACCESS_TOKEN = "$.accessToken";
  private static final String JSON_PATH_REFRESH_TOKEN = "$.refreshToken";

  private void setId(Object entity, Long id) {
    Class<?> clazz = entity.getClass();
    while (clazz != null) {
      try {
        var field = clazz.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
        return;
      } catch (NoSuchFieldException | IllegalAccessException e) {
        clazz = clazz.getSuperclass();
      }
    }
    throw new RuntimeException("id field not found in class hierarchy");
  }

  // ========== register ==========

  @Test
  void register_validRequest_returns201() throws Exception {
    RegisterResponseDTO mockResponse = new RegisterResponseDTO(
        "Cuenta creada correctamente. Revisa tu correo para confirmar tu cuenta.",
        "READER");
    when(authService.register(any(RegisterRequestDTO.class))).thenReturn(mockResponse);

    mockMvc.perform(post("/api/auth/register")
        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
        .content("""
            {
              "fullName": "Ada Lovelace",
              "email": "ada@example.com",
              "password": "Password1",
              "confirmPassword": "Password1",
              "userRole": "READER",
              "username": "ada"
            }
            """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Cuenta creada correctamente. Revisa tu correo para confirmar tu cuenta."))
        .andExpect(jsonPath("$.userRole").value("READER"));
  }

  // ========== login ==========

  @Test
  void login_validCredentials_returns200WithTokens() throws Exception {
    LoginResponseDTO mockResponse = new LoginResponseDTO(
        "Inicio de sesión exitoso.",
        "mock-access-token-123",
        "mock-refresh-token-456");
    when(authService.login(any(LoginRequestDTO.class), anyString())).thenReturn(mockResponse);

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "ada@example.com",
              "password": "Password1",
              "rememberMe": true
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Inicio de sesión exitoso."))
        .andExpect(jsonPath("$.accessToken").value("mock-access-token-123"))
        .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token-456"))
        .andExpect(result -> {
          String setCookie = result.getResponse().getHeader("Set-Cookie");
          assert setCookie != null && setCookie.contains("refreshToken=");
        });
  }

  @Test
  void login_invalidCredentials_returns400() throws Exception {
    when(authService.login(any(LoginRequestDTO.class), anyString()))
        .thenThrow(new ValidationException(List.of("Credenciales inválidas.")));

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "nobody@example.com",
              "password": "Password1"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0]").value("Credenciales inválidas."));
  }

  // ========== logout ==========

  @Test
  void logout_withRefreshTokenCookie_returns200AndDeletesCookie() throws Exception {
    doNothing().when(authService).logout("test-token-123");

    mockMvc.perform(post("/api/auth/logout")
        .cookie(new Cookie("refreshToken", "test-token-123")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Sesión cerrada"))
        .andExpect(result -> {
          String setCookie = result.getResponse().getHeader("Set-Cookie");
          assert setCookie != null && setCookie.contains("refreshToken=") && setCookie.contains("Max-Age=0");
        });

    verify(authService, times(1)).logout("test-token-123");
  }

  @Test
  void logout_withNoRefreshToken_returns200() throws Exception {
    mockMvc.perform(post("/api/auth/logout"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Sesión cerrada"));
  }

  // ========== me ==========

  @Test
  void getMyProfile_withValidToken_returns200() throws Exception {
    String token = "valid-token";
    User user = new User("Test User", "test@example.com", "hash", UserRole.READER, "testuser", null, null);
    setId(user, 1L);

    when(jwtService.isValid(token)).thenReturn(true);
    when(jwtService.extractEmail(token)).thenReturn("test@example.com");
    when(authService.getProfile("test@example.com")).thenReturn(
        new ProfileResponseDTO(1L, "test@example.com", "Test User", "READER", "testuser", null, null));

    mockMvc.perform(get("/api/auth/me")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.fullName").value("Test User"))
        .andExpect(jsonPath("$.userRole").value("READER"));
  }

  @Test
  void getMyProfile_withMissingToken_returns401() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized());
  }

  // ========== me (PUT) ==========

  @Test
  void updateMyProfile_withValidToken_returns200() throws Exception {
    String token = "valid-token";
    User user = new User("Updated User", "test@example.com", "hash", UserRole.READER, "updated", null, "My bio");
    setId(user, 1L);

    when(jwtService.isValid(token)).thenReturn(true);
    when(jwtService.extractEmail(token)).thenReturn("test@example.com");
    when(authService.updateProfile(eq("test@example.com"), any(ProfileUpdateRequestDTO.class)))
        .thenReturn(new ProfileResponseDTO(1L, "test@example.com", "Updated User", "READER", "updated", null, "My bio"));

    mockMvc.perform(put("/api/auth/me")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "username": "updated",
              "biography": "My bio"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("updated"))
        .andExpect(jsonPath("$.biography").value("My bio"));
  }

  @Test
  void updateMyProfile_withInvalidToken_returns401() throws Exception {
    when(jwtService.extractEmail("bad-token")).thenThrow(new UnauthorizedException("Token inválido"));

    mockMvc.perform(put("/api/auth/me")
        .header("Authorization", "Bearer bad-token")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "username": "updated"
            }
            """))
        .andExpect(status().isUnauthorized());
  }

  // ========== change-password ==========

  @Test
  void changePassword_withValidRequest_returns200() throws Exception {
    String token = "valid-token";
    User user = new User("Test User", "test@example.com", "hash", UserRole.READER, "testuser", null, null);
    setId(user, 1L);

    when(jwtService.isValid(token)).thenReturn(true);
    when(jwtService.extractEmail(token)).thenReturn("test@example.com");
    doNothing().when(authService).changePassword("test@example.com", "OldPass1", "NewPass1");

    mockMvc.perform(post("/api/auth/change-password")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "currentPassword": "OldPass1",
              "newPassword": "NewPass1",
              "confirmPassword": "NewPass1"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Contraseña actualizada correctamente"));
  }

  @Test
  void changePassword_withMissingToken_returns401() throws Exception {
    mockMvc.perform(post("/api/auth/change-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "currentPassword": "OldPass1",
              "newPassword": "NewPass1",
              "confirmPassword": "NewPass1"
            }
            """))
        .andExpect(status().isUnauthorized());
  }

  // ========== refresh ==========

  @Test
  void refresh_withValidRefreshToken_returns200WithNewTokens() throws Exception {
    LoginResponseDTO mockResponse = new LoginResponseDTO(
        "Sesión renovada.",
        "new-access-token",
        "new-refresh-token");
    when(authService.refresh("valid-refresh-token")).thenReturn(mockResponse);

    mockMvc.perform(post("/api/auth/refresh")
        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Sesión renovada."))
        .andExpect(jsonPath("$.accessToken").value("new-access-token"))
        .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
  }

  @Test
  void refresh_withMissingRefreshToken_returns401() throws Exception {
    when(authService.refresh(null)).thenThrow(new UnauthorizedException("Refresh token no encontrado"));

    mockMvc.perform(post("/api/auth/refresh"))
        .andExpect(status().isUnauthorized());
  }
}
package com.lectuaria.backend.controller;

import com.lectuaria.backend.dto.auth.LoginRequestDTO;
import com.lectuaria.backend.dto.auth.LoginResponseDTO;
import com.lectuaria.backend.dto.auth.RegisterRequestDTO;
import com.lectuaria.backend.dto.auth.RegisterResponseDTO;
import java.util.Objects;
import com.lectuaria.backend.exception.ValidationException;
import com.lectuaria.backend.service.auth.IAuthService;
import com.lectuaria.backend.security.JwtService;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.security.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private IAuthService authService;
  
  @MockBean
  private JwtService jwtService;

  private static final String JSON_PATH_ACCESS_TOKEN = "$.accessToken";
  private static final String JSON_PATH_REFRESH_TOKEN = "$.refreshToken";

  @Test
  void registerAndLoginSuccessfully() throws Exception {
    // Mock para register
    RegisterResponseDTO mockRegisterResponse = new RegisterResponseDTO(
        "Cuenta creada correctamente. Revisa tu correo para confirmar tu cuenta.",
        "normal");
    when(authService.register(any(RegisterRequestDTO.class))).thenReturn(mockRegisterResponse);

    // Mock para login
    LoginResponseDTO mockLoginResponse = new LoginResponseDTO(
        "Inicio de sesión exitoso.",
        "mock-access-token-123",
        "mock-refresh-token-456");
    when(authService.login(any(LoginRequestDTO.class))).thenReturn(mockLoginResponse);

    // 1. Verificar respuesta de REGISTER
    mockMvc.perform(post("/api/auth/register")
        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
        .content("""
            {
              "fullName": "Ada Lovelace",
              "email": "ada@example.com",
              "password": "Password1",
              "confirmPassword": "Password1",
              "userRole": "NORMAL",
              "username": "ada"
            }
            """))
        .andExpect(status().isCreated())
        .andExpect(
            jsonPath("$.message").value("Cuenta creada correctamente. Revisa tu correo para confirmar tu cuenta."))
        .andExpect(jsonPath("$.userRole").value("normal"));

    // 2. Verificar respuesta de LOGIN
    mockMvc.perform(post("/api/auth/login")
        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
        .content("""
            {
              "email": "ada@example.com",
              "password": "Password1",
              "rememberMe": true
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Inicio de sesión exitoso."))
        .andExpect(jsonPath(JSON_PATH_ACCESS_TOKEN).value("mock-access-token-123"))
        .andExpect(jsonPath(JSON_PATH_REFRESH_TOKEN).value("mock-refresh-token-456"));
  }

  @Test
  void rejectInvalidCredentials() throws Exception {
    when(authService.login(any(LoginRequestDTO.class)))
        .thenThrow(new ValidationException(java.util.List.of("Credenciales inválidas.")));

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

  @Test
  void denyProtectedRouteWithoutSession() throws Exception {
    // Mock JwtService para evitar errores de 500
    when(jwtService.extractEmail(any())).thenThrow(new RuntimeException("Invalid token"));
    
    // Este test no depende de AuthService porque extractEmail lanza excepción antes
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutInvalidatesSession() throws Exception {
    doNothing().when(authService).logout(anyString());

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
}
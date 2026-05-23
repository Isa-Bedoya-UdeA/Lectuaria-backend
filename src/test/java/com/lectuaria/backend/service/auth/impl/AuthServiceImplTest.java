package com.lectuaria.backend.service.auth.impl;

import com.lectuaria.backend.dto.auth.LoginRequestDTO;
import com.lectuaria.backend.dto.auth.LoginResponseDTO;
import com.lectuaria.backend.dto.auth.ProfileResponseDTO;
import com.lectuaria.backend.dto.auth.ProfileUpdateRequestDTO;
import com.lectuaria.backend.dto.auth.RegisterRequestDTO;
import com.lectuaria.backend.dto.auth.RegisterResponseDTO;
import com.lectuaria.backend.dto.library.LibraryRequestDTO;
import com.lectuaria.backend.exception.ConflictException;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.exception.ValidationException;
import com.lectuaria.backend.model.auth.RefreshToken;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.auth.LoginAttempt;
import com.lectuaria.backend.model.library.Librarian;
import com.lectuaria.backend.model.library.Library;
import com.lectuaria.backend.model.LivingZone;
import com.lectuaria.backend.repository.auth.RefreshTokenRepository;
import com.lectuaria.backend.repository.auth.LoginAttemptRepository;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.repository.library.LibraryRepository;
import com.lectuaria.backend.repository.LivingZoneRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.auth.IEmailService;
import com.lectuaria.backend.service.list.IUserListService;
import com.lectuaria.backend.service.notification.INotificationPreferenceService;
import com.lectuaria.backend.validation.RegisterBusinessValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private LibraryRepository libraryRepository;
    @Mock private LibrarianRepository librarianRepository;
    @Mock private LoginAttemptRepository loginAttemptRepository;
    @Mock private LivingZoneRepository livingZoneRepository;
    @Mock private IEmailService emailService;
    @Mock private RegisterBusinessValidator validator;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private IUserListService userListService;
    @Mock private INotificationPreferenceService notificationPreferenceService;

    private AuthServiceImpl authService;

    private void setId(Object entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, libraryRepository, librarianRepository,
                loginAttemptRepository, livingZoneRepository,
                emailService, validator, passwordEncoder,
                jwtService, refreshTokenRepository,
                userListService, notificationPreferenceService);
    }

    // ===== REGISTER TESTS =====

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("registers reader successfully")
        void register_readerSuccess() throws Exception {
            RegisterRequestDTO request = new RegisterRequestDTO();
            request.setFullName("Carlos Perez");
            request.setEmail("carlos@test.com");
            request.setPassword("Test1234");
            request.setConfirmPassword("Test1234");
            request.setUserRole(UserRole.READER);
            request.setUsername("carlosperez");

            when(validator.validate(request)).thenReturn(List.of());
            when(userRepository.findByEmail("carlos@test.com")).thenReturn(Optional.empty());
            when(userRepository.existsByUsernameIgnoreCase("carlosperez")).thenReturn(false);
            when(passwordEncoder.encode("Test1234")).thenReturn("encoded123");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                setId(u, 1L);
                return u;
            });

            RegisterResponseDTO response = authService.register(request);

            assertEquals("Cuenta creada correctamente. Revisa tu correo para confirmar tu cuenta.", response.getMessage());
            assertEquals("READER", response.getUserRole());
            verify(emailService).sendRegistrationConfirmation("carlos@test.com", "carlosperez");
            verify(userListService).createDefaultLists(any(User.class));
        }

        @Test
        @DisplayName("rejects when email already registered")
        void register_emailConflict() {
            RegisterRequestDTO request = new RegisterRequestDTO();
            request.setFullName("Carlos Perez");
            request.setEmail("carlos@test.com");
            request.setPassword("Test1234");
            request.setConfirmPassword("Test1234");
            request.setUserRole(UserRole.READER);
            request.setUsername("carlosperez");

            when(validator.validate(request)).thenReturn(List.of());
            when(userRepository.findByEmail("carlos@test.com")).thenReturn(Optional.of(new User()));

            ConflictException ex = assertThrows(ConflictException.class, () -> authService.register(request));
            assertEquals("El correo ya está registrado.", ex.getMessage());
        }

        @Test
        @DisplayName("rejects when username already taken")
        void register_usernameConflict() {
            RegisterRequestDTO request = new RegisterRequestDTO();
            request.setFullName("Carlos Perez");
            request.setEmail("carlos@test.com");
            request.setPassword("Test1234");
            request.setConfirmPassword("Test1234");
            request.setUserRole(UserRole.READER);
            request.setUsername("carlosperez");

            when(validator.validate(request)).thenReturn(List.of());
            when(userRepository.findByEmail("carlos@test.com")).thenReturn(Optional.empty());
            when(userRepository.existsByUsernameIgnoreCase("carlosperez")).thenReturn(true);

            ConflictException ex = assertThrows(ConflictException.class, () -> authService.register(request));
            assertEquals("El nombre de usuario ya está en uso.", ex.getMessage());
        }

        @Test
        @DisplayName("rejects when validation errors exist")
        void register_validationErrors() {
            RegisterRequestDTO request = new RegisterRequestDTO();
            request.setFullName("Carlos");
            request.setEmail("carlos@test.com");
            request.setPassword("weak");
            request.setConfirmPassword("weak");
            request.setUserRole(UserRole.READER);
            request.setUsername("carlosperez");

            when(validator.validate(request)).thenReturn(List.of("La contraseña es débil."));

            ValidationException ex = assertThrows(ValidationException.class, () -> authService.register(request));
            assertTrue(ex.getErrors().contains("La contraseña es débil."));
        }

        @Test
        @DisplayName("registers librarian and creates library")
        void register_librarianCreatesLibrary() throws Exception {
            LibraryRequestDTO libReq = new LibraryRequestDTO();
            libReq.setName("Biblioteca Central");
            libReq.setAddress("Calle 123");
            libReq.setContactEmail("bib@central.com");
            libReq.setContactPhone("5551234");
            libReq.setOpeningHours("8am-6pm");
            libReq.setIdZone(1L);

            RegisterRequestDTO request = new RegisterRequestDTO();
            request.setFullName("Librarian One");
            request.setEmail("librarian@test.com");
            request.setPassword("Test1234");
            request.setConfirmPassword("Test1234");
            request.setUserRole(UserRole.LIBRARIAN);
            request.setLibrary(libReq);

            when(validator.validate(request)).thenReturn(List.of());
            when(userRepository.findByEmail("librarian@test.com")).thenReturn(Optional.empty());
            // For librarian, username derived from library name
            when(userRepository.existsByUsernameIgnoreCase("biblioteca_central")).thenReturn(false);
            when(passwordEncoder.encode("Test1234")).thenReturn("encoded");
            when(libraryRepository.existsByContactEmail("bib@central.com")).thenReturn(false);
            when(libraryRepository.save(any(Library.class))).thenAnswer(inv -> {
                Library l = inv.getArgument(0);
                setId(l, 10L);
                return l;
            });
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                setId(u, 2L);
                return u;
            });
            when(librarianRepository.save(any(Librarian.class))).thenAnswer(inv -> inv.getArgument(0));

            RegisterResponseDTO response = authService.register(request);

            assertEquals("LIBRARIAN", response.getUserRole());
            verify(libraryRepository).save(any(Library.class));
            verify(librarianRepository).save(any(Librarian.class));
            verify(emailService).sendRegistrationConfirmation("librarian@test.com", "biblioteca_central");
        }

        @Test
        @DisplayName("rejects librarian when library contact email already in use")
        void register_librarianContactEmailConflict() {
            LibraryRequestDTO libReq = new LibraryRequestDTO();
            libReq.setName("Biblioteca Central");
            libReq.setAddress("Calle 123");
            libReq.setContactEmail("bib@existing.com");
            libReq.setContactPhone("5551234");
            libReq.setOpeningHours("8am-6pm");
            libReq.setIdZone(1L);

            RegisterRequestDTO request = new RegisterRequestDTO();
            request.setFullName("Librarian");
            request.setEmail("librarian@test.com");
            request.setPassword("Test1234");
            request.setConfirmPassword("Test1234");
            request.setUserRole(UserRole.LIBRARIAN);
            request.setLibrary(libReq);

            when(validator.validate(request)).thenReturn(List.of());
            when(userRepository.findByEmail("librarian@test.com")).thenReturn(Optional.empty());
            when(userRepository.existsByUsernameIgnoreCase("biblioteca_central")).thenReturn(false);
            when(libraryRepository.existsByContactEmail("bib@existing.com")).thenReturn(true);

            ConflictException ex = assertThrows(ConflictException.class, () -> authService.register(request));
            assertEquals("El correo de la biblioteca ya está registrado.", ex.getMessage());
        }
    }

    // ===== LOGIN TESTS =====

    @Nested
    @DisplayName("login()")
    class LoginTests {

        private User createTestUser(Long id, String email, String passwordHash, UserRole role) throws Exception {
            User user = new User("Test User", email, passwordHash, role, "testuser", null, null);
            setId(user, id);
            return user;
        }

        @Test
        @DisplayName("logs in successfully")
        void login_success() throws Exception {
            User user = createTestUser(1L, "user@test.com", "hashedpwd", UserRole.READER);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(loginAttemptRepository.countFailedSince(eq(1L), any(Instant.class))).thenReturn(0L);
            when(passwordEncoder.matches("password123", "hashedpwd")).thenReturn(true);
            when(jwtService.generateAccessToken("user@test.com", "READER")).thenReturn("access-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> {
                RefreshToken rt = inv.getArgument(0);
                setId(rt, 1L);
                return rt;
            });

            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("user@test.com");
            request.setPassword("password123");

            LoginResponseDTO response = authService.login(request, "127.0.0.1");

            assertEquals("Inicio de sesión exitoso.", response.getMessage());
            assertEquals("access-token", response.getAccessToken());
            assertNotNull(response.getRefreshToken());
            verify(loginAttemptRepository).save(any(LoginAttempt.class));
            verify(userListService).createDefaultLists(user);
        }

        @Test
        @DisplayName("rejects when user not found")
        void login_userNotFound() {
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("unknown@test.com");
            request.setPassword("password");

            ValidationException ex = assertThrows(ValidationException.class, () -> authService.login(request, "127.0.0.1"));
            assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Credenciales inválidas")));
        }

        @Test
        @DisplayName("rejects when password is wrong")
        void login_wrongPassword() throws Exception {
            User user = createTestUser(1L, "user@test.com", "hashedpwd", UserRole.READER);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(loginAttemptRepository.countFailedSince(eq(1L), any(Instant.class))).thenReturn(0L);
            when(passwordEncoder.matches("wrongpassword", "hashedpwd")).thenReturn(false);
            when(loginAttemptRepository.save(any(LoginAttempt.class))).thenAnswer(inv -> {
                LoginAttempt la = inv.getArgument(0);
                setId(la, 1L);
                return la;
            });
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("user@test.com");
            request.setPassword("wrongpassword");

            ValidationException ex = assertThrows(ValidationException.class, () -> authService.login(request, "127.0.0.1"));
            assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Credenciales inválidas")));
        }

@Test
        @DisplayName("locks account after too many failed attempts via count")
        void login_lockedByFailedCount() throws Exception {
            User user = createTestUser(1L, "user@test.com", "hashedpwd", UserRole.READER);
            // User's lockedUntil is null, but DB count is >= 5
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(loginAttemptRepository.countFailedSince(eq(1L), any(Instant.class))).thenReturn(5L);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("user@test.com");
            request.setPassword("wrongpassword");

            ValidationException ex = assertThrows(ValidationException.class, () -> authService.login(request, "127.0.0.1"));
            assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("bloqueada")));
            assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("15 minutos")));
        }

        @Test
        @DisplayName("shows remaining attempts warning on wrong password")
        void login_showsRemainingAttemptsWarning() throws Exception {
            User user = createTestUser(1L, "user@test.com", "hashedpwd", UserRole.READER);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            // After 4 failed, 1 attempt left
            when(loginAttemptRepository.countFailedSince(eq(1L), any(Instant.class)))
                    .thenReturn(4L)  // first check (4 < 5, no lockout yet)
                    .thenReturn(5L); // after wrong password → count hits 5 → locks
            when(passwordEncoder.matches("wrongpassword", "hashedpwd")).thenReturn(false);
            when(loginAttemptRepository.save(any(LoginAttempt.class))).thenAnswer(inv -> {
                LoginAttempt la = inv.getArgument(0);
                setId(la, 1L);
                return la;
            });
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("user@test.com");
            request.setPassword("wrongpassword");

            ValidationException ex = assertThrows(ValidationException.class, () -> authService.login(request, "127.0.0.1"));
            // After wrong password, count 4→5, remaining=0 → "bloqueada"
            assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("bloqueada")));
        }

        @Test
        @DisplayName("rejects already locked account (db lock)")
        void login_alreadyLocked() throws Exception {
            User user = createTestUser(1L, "user@test.com", "hashedpwd", UserRole.READER);
            user.setLockedUntil(Instant.now().plus(5, ChronoUnit.MINUTES));

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(loginAttemptRepository.countFailedSince(eq(1L), any(Instant.class))).thenReturn(3L);

            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("user@test.com");
            request.setPassword("password123");

            ValidationException ex = assertThrows(ValidationException.class, () -> authService.login(request, "127.0.0.1"));
            assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("bloqueada")));
        }

        @Test
        @DisplayName("shows remaining attempts on wrong password")
        void login_showsRemainingAttempts() throws Exception {
            User user = createTestUser(1L, "user@test.com", "hashedpwd", UserRole.READER);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            // After 4 failed, only 1 attempt left (count stays at 4, no lockout)
            when(loginAttemptRepository.countFailedSince(eq(1L), any(Instant.class))).thenReturn(4L);
            when(passwordEncoder.matches("wrongpassword", "hashedpwd")).thenReturn(false);
            when(loginAttemptRepository.save(any(LoginAttempt.class))).thenAnswer(inv -> {
                LoginAttempt la = inv.getArgument(0);
                setId(la, 1L);
                return la;
            });
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginRequestDTO request = new LoginRequestDTO();
            request.setEmail("user@test.com");
            request.setPassword("wrongpassword");

            ValidationException ex = assertThrows(ValidationException.class, () -> authService.login(request, "127.0.0.1"));
            assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("1 intentos")));
        }
    }

    // ===== LOGOUT TESTS =====

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("logs out successfully with valid token")
        void logout_success() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);
            RefreshToken token = new RefreshToken(user, "valid-token", Instant.now().plusSeconds(3600));
            setId(token, 1L);

            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            authService.logout("valid-token");

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            assertTrue(captor.getValue().isRevoked());
        }

        @Test
        @DisplayName("rejects invalid token")
        void logout_invalidToken() {
            when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.logout("invalid-token"));
            assertEquals("Token inválido", ex.getMessage());
        }

        @Test
        @DisplayName("rejects revoked token")
        void logout_revokedToken() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);
            RefreshToken token = new RefreshToken(user, "revoked-token", Instant.now().plusSeconds(3600));
            token.revoke();
            setId(token, 1L);

            when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(token));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.logout("revoked-token"));
            assertEquals("Token inválido o expirado", ex.getMessage());
        }

        @Test
        @DisplayName("rejects expired token")
        void logout_expiredToken() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);
            // Expired token: expiresAt in the past
            RefreshToken token = new RefreshToken(user, "expired-token", Instant.now().minusSeconds(3600));
            setId(token, 1L);

            when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.logout("expired-token"));
            assertEquals("Token inválido o expirado", ex.getMessage());
        }
    }

    // ===== REFRESH TESTS =====

    @Nested
    @DisplayName("refresh()")
    class RefreshTests {

        @Test
        @DisplayName("refreshes token successfully")
        void refresh_success() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);
            RefreshToken token = new RefreshToken(user, "valid-refresh", Instant.now().plusSeconds(3600));
            setId(token, 1L);

            when(refreshTokenRepository.findByToken("valid-refresh")).thenReturn(Optional.of(token));
            when(jwtService.generateAccessToken("test@test.com", "READER")).thenReturn("new-access-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginResponseDTO response = authService.refresh("valid-refresh");

            assertEquals("Token renovado", response.getMessage());
            assertEquals("new-access-token", response.getAccessToken());
            assertNotNull(response.getRefreshToken());
            assertTrue(token.isRevoked()); // old token revoked
        }

        @Test
        @DisplayName("rejects invalid refresh token")
        void refresh_invalidToken() {
            when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

            UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.refresh("invalid"));
            assertEquals("Token inválido", ex.getMessage());
        }

        @Test
        @DisplayName("rejects revoked refresh token")
        void refresh_revokedToken() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);
            RefreshToken token = new RefreshToken(user, "revoked", Instant.now().plusSeconds(3600));
            token.revoke();
            setId(token, 1L);

            when(refreshTokenRepository.findByToken("revoked")).thenReturn(Optional.of(token));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.refresh("revoked"));
            assertEquals("Refresh token inválido", ex.getMessage());
        }
    }

    // ===== GET PROFILE TESTS =====

    @Nested
    @DisplayName("getProfile()")
    class GetProfileTests {

        @Test
        @DisplayName("returns reader profile")
        void getProfile_reader() throws Exception {
            User user = new User("Reader Name", "reader@test.com", "hash", UserRole.READER, "reader", "http://photo.jpg", "Bio text");
            setId(user, 1L);

            when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(user));

            ProfileResponseDTO profile = authService.getProfile("reader@test.com");

            assertEquals(1L, profile.getId());
            assertEquals("reader@test.com", profile.getEmail());
            assertEquals("Reader Name", profile.getFullName());
            assertEquals("READER", profile.getUserRole());
            assertEquals("reader", profile.getUsername());
            assertEquals("http://photo.jpg", profile.getPhotoUrl());
            assertEquals("Bio text", profile.getBiography());
            // Library fields should be null for reader
            assertNull(profile.getLibraryName());
        }

        @Test
        @DisplayName("returns librarian profile with library info")
        void getProfile_librarian() throws Exception {
            User user = new User("Librarian Name", "librarian@test.com", "hash", UserRole.LIBRARIAN, "libname", null, null);
            setId(user, 2L);

            Library library = new Library("Central Library", "Desc", "123 Main St", "lib@email.com", "555-0000", "9-5", 1L);
            setId(library, 5L);

            Librarian librarian = new Librarian(user, library, "lib@email.com");
            setId(librarian, 1L);

            LivingZone zone = new LivingZone();
            zone.setName("Zona Norte");
            setId(zone, 1L);

            when(userRepository.findByEmail("librarian@test.com")).thenReturn(Optional.of(user));
            when(librarianRepository.findByUser(user)).thenReturn(Optional.of(librarian));
            when(livingZoneRepository.findById(1L)).thenReturn(Optional.of(zone));

            ProfileResponseDTO profile = authService.getProfile("librarian@test.com");

            assertEquals(2L, profile.getId());
            assertEquals("LIBRARIAN", profile.getUserRole());
            assertEquals("Central Library", profile.getLibraryName());
            assertEquals("123 Main St", profile.getLibraryAddress());
            assertEquals("lib@email.com", profile.getLibraryContactEmail());
            assertEquals("Zona Norte", profile.getLibraryZoneName());
        }

        @Test
        @DisplayName("throws when user not found")
        void getProfile_userNotFound() {
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.getProfile("unknown@test.com"));
            assertEquals("Usuario no encontrado", ex.getMessage());
        }
    }

    // ===== UPDATE PROFILE TESTS =====

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfileTests {

        @Test
        @DisplayName("updates reader profile successfully")
        void updateProfile_readerSuccess() throws Exception {
            User user = new User("Old Name", "reader@test.com", "hash", UserRole.READER, "oldusername", null, null);
            setId(user, 1L);

            ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
            request.setUsername("newusername");
            request.setPhotoUrl("http://newphoto.jpg");
            request.setBiography("New bio");

            when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(user));
            when(userRepository.existsByUsernameIgnoreCaseAndEmailNot("newusername", "reader@test.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            ProfileResponseDTO profile = authService.updateProfile("reader@test.com", request);

            assertEquals("newusername", profile.getUsername());
            assertEquals("http://newphoto.jpg", profile.getPhotoUrl());
            assertEquals("New bio", profile.getBiography());
        }

        @Test
        @DisplayName("rejects duplicate username for reader")
        void updateProfile_readerDuplicateUsername() throws Exception {
            User user = new User("Name", "reader@test.com", "hash", UserRole.READER, "oldusername", null, null);
            setId(user, 1L);

            ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
            request.setUsername("takenusername");

            when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(user));
            when(userRepository.existsByUsernameIgnoreCaseAndEmailNot("takenusername", "reader@test.com")).thenReturn(true);

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> authService.updateProfile("reader@test.com", request));
            assertEquals("El nombre de usuario ya está en uso.", ex.getMessage());
        }

        @Test
        @DisplayName("updates librarian profile with library info")
        void updateProfile_librarianSuccess() throws Exception {
            User user = new User("Old Lib Name", "lib@test.com", "hash", UserRole.LIBRARIAN, "libuser", null, null);
            setId(user, 1L);

            Library library = new Library("Old Library", "Desc", "Old Address", "old@lib.com", "111", "9-5", 1L);
            setId(library, 5L);

            Librarian librarian = new Librarian(user, library, "old@lib.com");
            setId(librarian, 1L);

            ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
            request.setUsername("newlibuser");
            request.setLibraryName("New Library Name");
            request.setLibraryLocation("New Address 45");
            request.setContactInfo("new@lib.com");
            request.setContactPhone("999");
            request.setOfficeHours("10-6");
            request.setIdZone(2L);

            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(user));
            when(userRepository.existsByUsernameIgnoreCaseAndEmailNot("newlibuser", "lib@test.com")).thenReturn(false);
            when(librarianRepository.findByUser(user)).thenReturn(Optional.of(librarian));
            when(libraryRepository.existsByContactEmail("new@lib.com")).thenReturn(false);
            when(livingZoneRepository.existsById(2L)).thenReturn(true);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(libraryRepository.save(any(Library.class))).thenAnswer(inv -> inv.getArgument(0));
            when(livingZoneRepository.findById(2L)).thenReturn(Optional.empty()); // zone name not needed for this test

            ProfileResponseDTO profile = authService.updateProfile("lib@test.com", request);

            assertEquals("newlibuser", profile.getUsername());
            assertEquals("New Library Name", profile.getLibraryName());
            assertEquals("New Address 45", profile.getLibraryAddress());
        }

        @Test
        @DisplayName("rejects librarian contact email already in use by another library")
        void updateProfile_librarianContactEmailConflict() throws Exception {
            User user = new User("Lib", "lib@test.com", "hash", UserRole.LIBRARIAN, "libuser", null, null);
            setId(user, 1L);

            Library library = new Library("My Library", "Desc", "Addr", "old@lib.com", "111", "9-5", 1L);
            setId(library, 5L);

            Librarian librarian = new Librarian(user, library, "old@lib.com");
            setId(librarian, 1L);

            ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
            request.setContactInfo("another@lib.com"); // already taken by another library

            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(user));
            when(librarianRepository.findByUser(user)).thenReturn(Optional.of(librarian));
            when(libraryRepository.existsByContactEmail("another@lib.com")).thenReturn(true);

            ConflictException ex = assertThrows(ConflictException.class,
                    () -> authService.updateProfile("lib@test.com", request));
            assertEquals("El correo de contacto ya está registrado.", ex.getMessage());
        }

        @Test
        @DisplayName("rejects invalid zone id for librarian")
        void updateProfile_librarianInvalidZone() throws Exception {
            User user = new User("Lib", "lib@test.com", "hash", UserRole.LIBRARIAN, "libuser", null, null);
            setId(user, 1L);

            Library library = new Library("My Library", "Desc", "Addr", "old@lib.com", "111", "9-5", 1L);
            setId(library, 5L);

            Librarian librarian = new Librarian(user, library, "old@lib.com");
            setId(librarian, 1L);

            ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
            request.setIdZone(999L);

            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(user));
            when(librarianRepository.findByUser(user)).thenReturn(Optional.of(librarian));
            when(livingZoneRepository.existsById(999L)).thenReturn(false);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> authService.updateProfile("lib@test.com", request));
            assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("zona/comuna")));
        }
    }

    // ===== CHANGE PASSWORD TESTS =====

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("changes password successfully")
        void changePassword_success() throws Exception {
            User user = new User("Test", "test@test.com", "oldhash", UserRole.READER, "test", null, null);
            setId(user, 1L);

            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("currentPass", "oldhash")).thenReturn(true);
            when(passwordEncoder.encode("newPass123")).thenReturn("newhash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> authService.changePassword("test@test.com", "currentPass", "newPass123"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("newhash", captor.getValue().getPasswordHash());
        }

        @Test
        @DisplayName("rejects when user not found")
        void changePassword_userNotFound() {
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.changePassword("unknown@test.com", "old", "new"));
            assertEquals("Usuario no encontrado", ex.getMessage());
        }

        @Test
        @DisplayName("rejects when current password is wrong")
        void changePassword_wrongCurrentPassword() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);

            when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> authService.changePassword("test@test.com", "wrong", "newPass"));
            assertEquals("Contraseña actual incorrecta", ex.getMessage());
        }
    }
}
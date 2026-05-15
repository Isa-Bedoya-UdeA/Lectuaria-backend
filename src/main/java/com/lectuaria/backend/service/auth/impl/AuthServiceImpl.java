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
import com.lectuaria.backend.model.library.Librarian;
import com.lectuaria.backend.model.library.Library;
import com.lectuaria.backend.model.LivingZone;
import com.lectuaria.backend.repository.auth.RefreshTokenRepository;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.repository.library.LibraryRepository;
import com.lectuaria.backend.repository.LivingZoneRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.auth.IEmailService;
import com.lectuaria.backend.service.auth.IAuthService;
import com.lectuaria.backend.service.list.IUserListService;
import com.lectuaria.backend.service.notification.INotificationPreferenceService;
import com.lectuaria.backend.validation.RegisterBusinessValidator;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final LibraryRepository libraryRepository;
    private final LibrarianRepository librarianRepository;
    private final IEmailService emailService;
    private final RegisterBusinessValidator validator;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final IUserListService userListService;
    private final LivingZoneRepository livingZoneRepository;
    private final INotificationPreferenceService notificationPreferenceService;

    public AuthServiceImpl(UserRepository userRepository,
            LibraryRepository libraryRepository,
            LibrarianRepository librarianRepository,
            LivingZoneRepository livingZoneRepository,
            IEmailService emailService,
            RegisterBusinessValidator validator,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository,
            IUserListService userListService,
            INotificationPreferenceService notificationPreferenceService) {
        this.userRepository = userRepository;
        this.libraryRepository = libraryRepository;
        this.librarianRepository = librarianRepository;
        this.livingZoneRepository = livingZoneRepository;
        this.emailService = emailService;
        this.validator = validator;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userListService = userListService;
        this.notificationPreferenceService = notificationPreferenceService;
    }

    @Override
    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        List<String> errors = validator.validate(request);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("El correo ya está registrado.",
                    List.of("Intenta iniciar sesión o recuperar tu contraseña."));
        }

        // Determinar username: para librarians, usar el nombre de la biblioteca
        String username = request.getUserRole() == UserRole.LIBRARIAN
                ? request.getLibrary().getName().toLowerCase().replaceAll("\\s+", "_")
                : request.getUsername();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("El nombre de usuario ya está en uso.", List.of("Prueba con otro nombre."));
        }

        // Crear usuario
        User user = new User(
                request.getFullName(),
                request.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                request.getUserRole(),
                username,
                null,
                null);
        userRepository.save(user);

        // Si es bibliotecario, crear Library y Librarian
        if (request.getUserRole() == UserRole.LIBRARIAN && request.getLibrary() != null) {
            createLibraryAndLibrarian(user, request.getLibrary());
        }

        emailService.sendRegistrationConfirmation(user.getEmail(), user.getUsername());

        // US-0030: Create default lists for readers
        userListService.createDefaultLists(user);

        // Create default notification preferences
        notificationPreferenceService.getUserPreferences(user.getId());

        return new RegisterResponseDTO(
                "Cuenta creada correctamente. Revisa tu correo para confirmar tu cuenta.",
                user.getRole().name());
    }

    @Transactional
    private void createLibraryAndLibrarian(User user, LibraryRequestDTO libRequest) {
        // Verificar que el email de contacto de la biblioteca no esté en uso
        if (libraryRepository.existsByContactEmail(libRequest.getContactEmail())) {
            throw new ConflictException("El correo de la biblioteca ya está registrado.",
                    List.of("Usa otro correo o contacta al administrador."));
        }

        // Crear Library
        Library library = new Library(
                libRequest.getName(),
                libRequest.getDescription(),
                libRequest.getAddress(),
                libRequest.getContactEmail(),
                libRequest.getContactPhone(),
                libRequest.getOpeningHours(),
                libRequest.getIdZone());
        libraryRepository.save(library);

        // Crear Librarian (relación User-Library)
        Librarian librarian = new Librarian(user, library, libRequest.getContactEmail());
        librarianRepository.save(librarian);
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException(List.of("Credenciales inválidas.")));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ValidationException(List.of("Credenciales inválidas."));
        }

        String accessToken = jwtService.generateAccessToken(email, user.getRole().name());
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken(
                user,
                refreshTokenValue,
                Instant.now().plusSeconds(60 * 60 * 24 * 30));

        refreshTokenRepository.save(refreshToken);

        // US-0030: Ensure existing users also get their default lists if missing
        userListService.createDefaultLists(user);

        // Ensure existing users also get their default notification preferences if missing
        notificationPreferenceService.getUserPreferences(user.getId());

        return new LoginResponseDTO(
                "Inicio de sesión exitoso.",
                accessToken,
                refreshTokenValue);
    }

    @Override
    public void logout(String refreshTokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new UnauthorizedException("Token inválido"));

        if (token.isExpired() || token.isRevoked()) {
            throw new UnauthorizedException("Token inválido o expirado");
        }

        token.revoke();
        refreshTokenRepository.save(token);
    }

    @Override
    public LoginResponseDTO refresh(String refreshTokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new UnauthorizedException("Token inválido"));

        if (token.isExpired() || token.isRevoked()) {
            throw new UnauthorizedException("Refresh token inválido");
        }

        User user = token.getUser();
        token.revoke();
        refreshTokenRepository.save(token);

        String newAccessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name());
        String newRefreshTokenValue = UUID.randomUUID().toString();

        RefreshToken newRefreshToken = new RefreshToken(
                user,
                newRefreshTokenValue,
                Instant.now().plusSeconds(60 * 60 * 24 * 30));

        refreshTokenRepository.save(newRefreshToken);

        return new LoginResponseDTO(
                "Token renovado",
                newAccessToken,
                newRefreshTokenValue);
    }

    // ========== MÉTODOS PARA PERFIL DE USUARIO ==========

    @Override
    public ProfileResponseDTO getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        if (user.getRole() == UserRole.LIBRARIAN) {
            return getLibrarianProfile(user);
        } else {
            return getReaderProfile(user);
        }
    }

    private ProfileResponseDTO getReaderProfile(User user) {
        return new ProfileResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getUsername(),
                user.getPhotoUrl(),
                user.getBiography());
    }

    @SuppressWarnings("null")
    private ProfileResponseDTO getLibrarianProfile(User user) {
        Librarian librarian = librarianRepository.findByUser(user)
                .orElseThrow(() -> new ValidationException(List.of("Perfil de bibliotecario no encontrado.")));

        Library library = librarian.getLibrary();

        String zoneName = null;
        if (library.getIdZone() != null) {
            zoneName = livingZoneRepository.findById(library.getIdZone())
                    .map(LivingZone::getName)
                    .orElse(null);
        }

        return new ProfileResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getUsername(),
                user.getPhotoUrl(),
                user.getBiography(),

                library.getName(),
                library.getAddress(),
                library.getContactEmail(),
                library.getContactPhone(),
                library.getOpeningHours(),
                library.getIdZone(),
                zoneName,
                library.getId());
    }

    @Override
    @Transactional
    public ProfileResponseDTO updateProfile(String email, ProfileUpdateRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException(List.of("Usuario no encontrado.")));

        if (user.getRole() == UserRole.LIBRARIAN) {
            return updateLibrarianProfile(user, request);
        }
        return updateReaderProfile(user, request);
    }

    @SuppressWarnings("null")
    private ProfileResponseDTO updateReaderProfile(User user, ProfileUpdateRequestDTO request) {
        // Validar y actualizar username si se proporciona
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (userRepository.existsByUsernameIgnoreCaseAndEmailNot(request.getUsername(), user.getEmail())) {
                throw new ConflictException("El nombre de usuario ya está en uso.",
                        List.of("Prueba con otro nombre."));
            }
            user.setUsername(request.getUsername());
        }

        // Actualizar campos opcionales (solo si no son null)
        if (request.getPhotoUrl() != null) {
            user.setPhotoUrl(request.getPhotoUrl());
        }
        if (request.getBiography() != null) {
            user.setBiography(request.getBiography());
        }

        userRepository.save(user);
        return getReaderProfile(user);
    }

    @SuppressWarnings("null")
    @Transactional
    private ProfileResponseDTO updateLibrarianProfile(User user, ProfileUpdateRequestDTO request) {
        // ===== Actualizar campos del usuario (comunes) =====
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (userRepository.existsByUsernameIgnoreCaseAndEmailNot(request.getUsername(), user.getEmail())) {
                throw new ConflictException("El nombre de usuario ya está en uso.",
                        List.of("Prueba con otro nombre."));
            }
            user.setUsername(request.getUsername());
        }

        if (request.getPhotoUrl() != null) {
            user.setPhotoUrl(request.getPhotoUrl());
        }
        if (request.getBiography() != null) {
            user.setBiography(request.getBiography());
        }

        // ===== Actualizar datos de la biblioteca =====
        Librarian librarian = librarianRepository.findByUser(user)
                .orElseThrow(() -> new ValidationException(List.of("Perfil de bibliotecario no encontrado.")));

        Library library = librarian.getLibrary();

        if (request.getLibraryName() != null && !request.getLibraryName().isBlank()) {
            library.setName(request.getLibraryName());
        }
        if (request.getLibraryLocation() != null && !request.getLibraryLocation().isBlank()) {
            library.setAddress(request.getLibraryLocation());
        }
        if (request.getContactInfo() != null && !request.getContactInfo().isBlank()) {
            // Validar que el nuevo email no esté en uso por otra biblioteca
            if (!request.getContactInfo().equalsIgnoreCase(library.getContactEmail())
                    && libraryRepository.existsByContactEmail(request.getContactInfo())) {
                throw new ConflictException("El correo de contacto ya está registrado.",
                        List.of("Usa otro correo o contacta al administrador."));
            }
            library.setContactEmail(request.getContactInfo());
        }
        if (request.getContactPhone() != null) {
            library.setContactPhone(request.getContactPhone());
        }
        if (request.getOfficeHours() != null) {
            library.setOpeningHours(request.getOfficeHours());
        }
        if (request.getIdZone() != null) {
            // Validar que la zona existe
            if (!livingZoneRepository.existsById(request.getIdZone())) {
                throw new ValidationException(List.of("La zona/comuna seleccionada no existe."));
            }
            library.setIdZone(request.getIdZone());
        }

        // Guardar cambios en ambas entidades
        userRepository.save(user);
        libraryRepository.save(library);

        return getLibrarianProfile(user);
    }
}

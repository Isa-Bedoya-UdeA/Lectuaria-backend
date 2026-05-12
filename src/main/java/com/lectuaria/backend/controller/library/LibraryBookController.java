// src/main/java/com/lectuaria/backend/controller/library/LibraryBookController.java
package com.lectuaria.backend.controller.library;

import com.lectuaria.backend.dto.book.BulkUploadResultDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.book.IBulkUploadService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/library-books")
public class LibraryBookController {

    private static final Logger logger = LoggerFactory.getLogger(LibraryBookController.class);
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final IBulkUploadService bulkUploadService;

    public LibraryBookController(
            UserRepository userRepository,
            JwtService jwtService,
            IBulkUploadService bulkUploadService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.bulkUploadService = bulkUploadService;
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<BulkUploadResultDTO> bulkUpload(
            @RequestParam("file") MultipartFile file) {

        // Obtener el usuario autenticado desde el contexto de seguridad de Spring
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("LibraryBookController: Bulk upload initiated by user: {}", authentication.getName());

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("LibraryBookController: User is not authenticated");
            throw new UnauthorizedException("Usuario no autenticado");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("LibraryBookController: User not found with email: {}", email);
                    return new UnauthorizedException("Usuario no encontrado");
                });

        logger.info("LibraryBookController: User found with email: {}, role: {}", email, user.getRole());

        if (user.getRole() != UserRole.LIBRARIAN) {
            logger.warn("LibraryBookController: User {} is not a librarian, role: {}", email, user.getRole());
            throw new UnauthorizedException("Acceso denegado: solo bibliotecarios pueden realizar esta acción");
        }

        Long userId = user.getId();
        logger.info("LibraryBookController: Processing CSV upload for user: {} with userId: {}", email, userId);
        return ResponseEntity.ok(bulkUploadService.processCsv(file, userId));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        String csv = "ISBN,Titulo,Autores,Generos,Descripcion,Editorial,Paginas,FechaPublicacion,PortadaUrl,Formato,CopiasFisicas\n" +
                     "9786078842841,Los Ojos del Perro Siberiano,Antonio Santaana,Novela infantil;Literatura juvenil,\"Novela que narra la historia de un adolescente que descubre que su hermano mayor tiene SIDA.\",Norma,144,1998-01-01,https://ejemplo.com/portada1.jpg,ambos,5\n" +
                     "9788496150257,Cien años de soledad,Gabriel Garcia Marquez,Realismo magico;Clasico,\"La historia de la familia Buendia a lo largo de siete generaciones en el pueblo ficticio de Macondo.\",Sudamericana,471,1967-05-30,https://ejemplo.com/portada2.jpg,fisico,10\n" +
                     "9788466333333,It (Eso),Stephen King,Terror;Suspenso,\"Varios niños de una pequeña ciudad de Maine se unen para combatir a una entidad malvada que asesina niños.\",Viking Press,1504,1986-09-15,https://ejemplo.com/portada3.jpg,digital,0";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lectuaria_template.csv")
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(csv.getBytes());
    }


}

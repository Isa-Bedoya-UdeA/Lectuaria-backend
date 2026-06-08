package com.lectuaria.backend.controller.library;

import com.lectuaria.backend.dto.book.BulkUploadResultDTO;
import com.lectuaria.backend.dto.library.LibraryBookAvailabilityDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.LibraryBook;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.repository.library.LibraryBookRepository;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.book.IBulkUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/library-books")
public class LibraryBookController {

    private static final Logger logger = LoggerFactory.getLogger(LibraryBookController.class);
    private final IBulkUploadService bulkUploadService;
    private final LibraryBookRepository libraryBookRepository;
    private final LibrarianRepository librarianRepository;
    private final AuthenticatedUserResolver userResolver;

    public LibraryBookController(
            IBulkUploadService bulkUploadService,
            LibraryBookRepository libraryBookRepository,
            LibrarianRepository librarianRepository,
            AuthenticatedUserResolver userResolver) {
        this.bulkUploadService = bulkUploadService;
        this.libraryBookRepository = libraryBookRepository;
        this.librarianRepository = librarianRepository;
        this.userResolver = userResolver;
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<EntityModel<BulkUploadResultDTO>> bulkUpload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        logger.info("Bulk upload initiated");
        User user = userResolver.requireCurrentUser(httpRequest);
        requireLibrarianRole(user);
        logger.info("Processing CSV upload for userId: {}", user.getId());
        BulkUploadResultDTO result = bulkUploadService.processCsv(file, user.getId());
        return ResponseEntity.ok(EntityModel.of(result,
                linkTo(methodOn(LibraryBookController.class).bulkUpload(file, httpRequest)).withSelfRel(),
                linkTo(methodOn(LibraryBookController.class).downloadTemplate()).withRel("template")));
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

    @PatchMapping("/{bookId}/availability")
    public ResponseEntity<EntityModel<LibraryBookAvailabilityDTO>> updateAvailability(
            @PathVariable Long bookId,
            @RequestBody LibraryBookAvailabilityDTO request,
            HttpServletRequest httpRequest) {
        User user = userResolver.requireCurrentUser(httpRequest);
        requireLibrarianRole(user);

        var librarian = librarianRepository.findByUser(user)
                .orElseThrow(() -> new UnauthorizedException("Perfil de bibliotecario no encontrado"));
        Long libraryId = librarian.getLibrary().getId();

        LibraryBook libraryBook = libraryBookRepository.findByLibraryIdAndBookId(libraryId, bookId)
                .orElseThrow(() -> new RuntimeException("Este libro no está en tu biblioteca"));

        if (request.getPhysicalCopies() != null) {
            libraryBook.setPhysicalCopies(request.getPhysicalCopies());
        }
        if (request.getDigitalAvailable() != null) {
            libraryBook.setDigitalAvailable(request.getDigitalAvailable());
        }
        if (request.getDigitalPlatformId() != null) {
            libraryBook.setDigitalPlatform(request.getDigitalPlatformId());
        }

        libraryBookRepository.save(libraryBook);
        logger.info("Availability updated for book {} in library {} by user {}", bookId, libraryId, user.getId());

        LibraryBookAvailabilityDTO response = new LibraryBookAvailabilityDTO(
                libraryBook.getPhysicalCopies(),
                libraryBook.getDigitalAvailable(),
                libraryBook.getDigitalPlatform());
        return ResponseEntity.ok(EntityModel.of(response,
                linkTo(methodOn(LibraryBookController.class).updateAvailability(bookId, request, httpRequest)).withSelfRel()));
    }

    private void requireLibrarianRole(User user) {
        if (user.getRole() != UserRole.LIBRARIAN) {
            throw new UnauthorizedException("Acceso denegado: solo bibliotecarios pueden realizar esta acción");
        }
    }
}

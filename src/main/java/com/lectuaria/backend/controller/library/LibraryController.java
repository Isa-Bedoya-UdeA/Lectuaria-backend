package com.lectuaria.backend.controller.library;

import com.lectuaria.backend.dto.library.LibrarySummaryDTO;
import com.lectuaria.backend.dto.statistics.LibraryStatisticsDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.library.ILibraryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/libraries")
public class LibraryController {

    private final ILibraryService libraryService;
    private final AuthenticatedUserResolver userResolver;

    public LibraryController(ILibraryService libraryService, AuthenticatedUserResolver userResolver) {
        this.libraryService = libraryService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<CollectionModel<LibrarySummaryDTO>> getAllLibraries() {
        return ResponseEntity.ok(CollectionModel.of(libraryService.getAllLibraries(),
                linkTo(methodOn(LibraryController.class).getAllLibraries()).withSelfRel()));
    }

    @GetMapping("/me/statistics")
    public ResponseEntity<EntityModel<LibraryStatisticsDTO>> getMyLibraryStatistics(HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        return ResponseEntity.ok(EntityModel.of(libraryService.getMyLibraryStatistics(user),
                linkTo(methodOn(LibraryController.class).getMyLibraryStatistics(request)).withSelfRel(),
                linkTo(methodOn(LibraryController.class).getAllLibraries()).withRel("libraries")));
    }
}

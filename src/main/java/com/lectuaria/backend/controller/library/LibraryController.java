package com.lectuaria.backend.controller.library;

import com.lectuaria.backend.dto.library.LibrarySummaryDTO;
import com.lectuaria.backend.service.library.ILibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/libraries")
public class LibraryController {

    private final ILibraryService libraryService;

    public LibraryController(ILibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping
    public ResponseEntity<List<LibrarySummaryDTO>> getAllLibraries() {
        return ResponseEntity.ok(libraryService.getAllLibraries());
    }
}

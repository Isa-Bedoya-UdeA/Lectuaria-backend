package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.book.GenreWithBookCountDTO;
import com.lectuaria.backend.service.book.GenreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public ResponseEntity<List<GenreDTO>> getAllGenres() {
        return ResponseEntity.ok(genreService.getAllGenres());
    }

    @GetMapping("/with-count")
    public ResponseEntity<List<GenreWithBookCountDTO>> getAllGenresWithBookCount() {
        return ResponseEntity.ok(genreService.getAllGenresWithBookCount());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenreDTO> getGenreById(@PathVariable Long id) {
        return ResponseEntity.ok(genreService.getGenreById(id));
    }
}
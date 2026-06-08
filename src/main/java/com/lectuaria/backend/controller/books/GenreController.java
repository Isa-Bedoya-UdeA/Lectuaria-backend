package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.book.GenreWithBookCountDTO;
import com.lectuaria.backend.service.book.GenreService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public ResponseEntity<CollectionModel<GenreDTO>> getAllGenres() {
        return ResponseEntity.ok(CollectionModel.of(genreService.getAllGenres(),
                linkTo(methodOn(GenreController.class).getAllGenres()).withSelfRel(),
                linkTo(methodOn(GenreController.class).getAllGenresWithBookCount()).withRel("with-count")));
    }

    @GetMapping("/with-count")
    public ResponseEntity<CollectionModel<GenreWithBookCountDTO>> getAllGenresWithBookCount() {
        return ResponseEntity.ok(CollectionModel.of(genreService.getAllGenresWithBookCount(),
                linkTo(methodOn(GenreController.class).getAllGenresWithBookCount()).withSelfRel(),
                linkTo(methodOn(GenreController.class).getAllGenres()).withRel("all")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<GenreDTO>> getGenreById(@PathVariable Long id) {
        return ResponseEntity.ok(EntityModel.of(genreService.getGenreById(id),
                linkTo(methodOn(GenreController.class).getGenreById(id)).withSelfRel(),
                linkTo(methodOn(GenreController.class).getAllGenres()).withRel("all")));
    }
}

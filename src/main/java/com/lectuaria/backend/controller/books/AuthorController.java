package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.AuthorDTO;
import com.lectuaria.backend.service.book.AuthorService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    public ResponseEntity<CollectionModel<AuthorDTO>> getAllAuthors() {
        return ResponseEntity.ok(CollectionModel.of(authorService.getAllAuthors(),
                linkTo(methodOn(AuthorController.class).getAllAuthors()).withSelfRel()));
    }
}

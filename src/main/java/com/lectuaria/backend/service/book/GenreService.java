package com.lectuaria.backend.service.book;

import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.book.GenreWithBookCountDTO;
import com.lectuaria.backend.model.book.Genre;
import com.lectuaria.backend.repository.book.GenreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GenreService {

    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    public List<GenreDTO> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(g -> new GenreDTO(g.getId(), g.getName(), g.getDescription()))
                .collect(Collectors.toList());
    }

    public List<GenreWithBookCountDTO> getAllGenresWithBookCount() {
        return genreRepository.findAllWithBookCount();
    }

    @SuppressWarnings("null")
    public GenreDTO getGenreById(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Género no encontrado"));
        return new GenreDTO(genre.getId(), genre.getName(), genre.getDescription());
    }
}
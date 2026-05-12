package com.lectuaria.backend.service.book;

import com.lectuaria.backend.dto.book.AuthorDTO;
import com.lectuaria.backend.repository.book.AuthorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<AuthorDTO> getAllAuthors() {
        return authorRepository.findAll().stream()
                .map(author -> new AuthorDTO(author.getId(), author.getName()))
                .collect(Collectors.toList());
    }
}

package com.lectuaria.backend.service.library.impl;

import com.lectuaria.backend.dto.library.LibrarySummaryDTO;
import com.lectuaria.backend.repository.library.LibraryRepository;
import com.lectuaria.backend.service.library.ILibraryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryServiceImpl implements ILibraryService {

    private final LibraryRepository libraryRepository;

    public LibraryServiceImpl(LibraryRepository libraryRepository) {
        this.libraryRepository = libraryRepository;
    }

    public List<LibrarySummaryDTO> getAllLibraries() {
        return libraryRepository.findAll().stream()
                .map(lib -> new LibrarySummaryDTO(
                        lib.getId(),
                        lib.getName(),
                        lib.getDescription(),
                        lib.getAddress(),
                        lib.getContactEmail(),
                        lib.getContactPhone(),
                        lib.getOpeningHours(),
                        null // Zone mapping if needed, but keeping it simple for now
                ))
                .collect(Collectors.toList());
    }
}

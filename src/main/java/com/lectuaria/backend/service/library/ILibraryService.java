package com.lectuaria.backend.service.library;

import com.lectuaria.backend.dto.library.LibrarySummaryDTO;

import java.util.List;

public interface ILibraryService {
    List<LibrarySummaryDTO> getAllLibraries();
}

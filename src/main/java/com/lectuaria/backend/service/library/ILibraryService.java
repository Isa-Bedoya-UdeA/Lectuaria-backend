package com.lectuaria.backend.service.library;

import com.lectuaria.backend.dto.library.LibrarySummaryDTO;
import com.lectuaria.backend.dto.statistics.LibraryStatisticsDTO;
import com.lectuaria.backend.model.auth.User;

import java.util.List;

public interface ILibraryService {
    List<LibrarySummaryDTO> getAllLibraries();
    LibraryStatisticsDTO getMyLibraryStatistics(User user);
}

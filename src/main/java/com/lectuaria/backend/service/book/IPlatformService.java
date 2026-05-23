package com.lectuaria.backend.service.book;

import com.lectuaria.backend.dto.book.PlatformDTO;
import java.util.List;

public interface IPlatformService {
    List<PlatformDTO> getAllPlatforms();
}
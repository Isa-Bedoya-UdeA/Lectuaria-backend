package com.lectuaria.backend.service.book.impl;

import com.lectuaria.backend.dto.book.PlatformDTO;
import com.lectuaria.backend.model.book.Platform;
import com.lectuaria.backend.repository.book.PlatformRepository;
import com.lectuaria.backend.service.book.IPlatformService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlatformServiceImpl implements IPlatformService {

    private final PlatformRepository platformRepository;

    public PlatformServiceImpl(PlatformRepository platformRepository) {
        this.platformRepository = platformRepository;
    }

    @Override
    public List<PlatformDTO> getAllPlatforms() {
        return platformRepository.findAll().stream()
                .map(p -> new PlatformDTO(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }
}
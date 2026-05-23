package com.lectuaria.backend.controller.book;

import com.lectuaria.backend.dto.book.PlatformDTO;
import com.lectuaria.backend.service.book.IPlatformService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platforms")
public class PlatformController {

    private final IPlatformService platformService;

    public PlatformController(IPlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping
    public List<PlatformDTO> getAllPlatforms() {
        return platformService.getAllPlatforms();
    }
}
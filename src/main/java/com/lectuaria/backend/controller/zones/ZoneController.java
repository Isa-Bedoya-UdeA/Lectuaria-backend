package com.lectuaria.backend.controller.zones;

import com.lectuaria.backend.model.LivingZone;
import com.lectuaria.backend.repository.LivingZoneRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final LivingZoneRepository zoneRepository;

    public ZoneController(LivingZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    @GetMapping
    public List<LivingZone> getAllZones() {
        return zoneRepository.findAll();
    }

    @SuppressWarnings("null")
    @GetMapping("/{id}")
    public LivingZone getZoneById(@PathVariable Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zona no encontrada"));
    }
}
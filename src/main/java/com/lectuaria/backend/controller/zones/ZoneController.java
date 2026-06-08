package com.lectuaria.backend.controller.zones;

import com.lectuaria.backend.dto.common.ZoneDTO;
import com.lectuaria.backend.exception.ResourceNotFoundException;
import com.lectuaria.backend.model.LivingZone;
import com.lectuaria.backend.repository.LivingZoneRepository;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final LivingZoneRepository zoneRepository;

    public ZoneController(LivingZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    @GetMapping
    public ResponseEntity<CollectionModel<ZoneDTO>> getAllZones() {
        List<ZoneDTO> zones = zoneRepository.findAll().stream()
                .map(ZoneController::toDto)
                .toList();
        CollectionModel<ZoneDTO> model = CollectionModel.of(zones);
        model.add(linkTo(methodOn(ZoneController.class).getAllZones()).withSelfRel());
        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<ZoneDTO>> getZoneById(@PathVariable Long id) {
        LivingZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zona no encontrada con id: " + id));
        EntityModel<ZoneDTO> model = EntityModel.of(toDto(zone));
        model.add(linkTo(methodOn(ZoneController.class).getZoneById(id)).withSelfRel());
        model.add(linkTo(methodOn(ZoneController.class).getAllZones()).withRel("all"));
        return ResponseEntity.ok(model);
    }

    private static ZoneDTO toDto(LivingZone zone) {
        return ZoneDTO.builder()
                .id(zone.getId())
                .name(zone.getName())
                .build();
    }
}

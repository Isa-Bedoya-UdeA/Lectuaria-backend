package com.lectuaria.backend.repository;

import com.lectuaria.backend.model.LivingZone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LivingZoneRepository extends JpaRepository<LivingZone, Long> {
    // Métodos básicos ya incluidos por JpaRepository:
    // - findById(Long id)
    // - findAll()
    // - existsById(Long id)
    // - save(LivingZone entity)
    // - deleteById(Long id)
}
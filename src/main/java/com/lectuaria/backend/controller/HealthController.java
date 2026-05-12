package com.lectuaria.backend.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/db")
    public Map<String, String> checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return Map.of(
                    "status", "UP",
                    "database", "PostgreSQL",
                    "message", "Conexión exitosa");
        } catch (SQLException e) {
            return Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "database", "PostgreSQL");
        }
    }

    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "lectuaria-backend");
    }
}
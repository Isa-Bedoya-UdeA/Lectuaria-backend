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

    // Claves de los campos del response JSON. Constantes para evitar
    // duplicacion de literales (regla S1192 de SonarCloud) y para que
    // un cambio de nombre (e.g. "status" -> "estado") sea un solo punto.
    private static final String KEY_STATUS = "status";
    private static final String KEY_DATABASE = "database";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ERROR = "error";
    private static final String KEY_SERVICE = "service";

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/db")
    public Map<String, String> checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return Map.of(
                    KEY_STATUS, STATUS_UP,
                    KEY_DATABASE, "PostgreSQL",
                    KEY_MESSAGE, "Conexión exitosa");
        } catch (SQLException e) {
            return Map.of(
                    KEY_STATUS, STATUS_DOWN,
                    KEY_ERROR, e.getMessage(),
                    KEY_DATABASE, "PostgreSQL");
        }
    }

    @GetMapping
    public Map<String, String> health() {
        return Map.of(KEY_STATUS, STATUS_UP, KEY_SERVICE, "lectuaria-backend");
    }
}
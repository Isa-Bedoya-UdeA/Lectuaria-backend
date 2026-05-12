package com.lectuaria.backend;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LectuariaBackendApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(LectuariaBackendApplication.class);

        // Forzar perfil dev explícitamente
        app.setAdditionalProfiles("dev");

        var ctx = app.run(args);
        System.out.println("✅ Perfiles activos: " + Arrays.toString(ctx.getEnvironment().getActiveProfiles()));
    }
}
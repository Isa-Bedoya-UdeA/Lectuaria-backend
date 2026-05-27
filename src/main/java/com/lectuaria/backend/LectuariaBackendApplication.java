package com.lectuaria.backend;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LectuariaBackendApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(LectuariaBackendApplication.class);

        // Solo forzar dev si no hay perfil activo (para desarrollo local)
        String[] activeProfiles = System.getProperty("spring.profiles.active", "").split(",");
        boolean hasActiveProfile = activeProfiles.length > 0 && !activeProfiles[0].isEmpty();
        
        if (!hasActiveProfile) {
            app.setAdditionalProfiles("dev");
        }

        var ctx = app.run(args);
        System.out.println("✅ Perfiles activos: " + Arrays.toString(ctx.getEnvironment().getActiveProfiles()));
    }
}
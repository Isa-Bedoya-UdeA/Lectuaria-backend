package com.lectuaria.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

@Configuration
public class CorsConfig {

        @Value("${frontend.url}")
        private String frontendUrl;

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowCredentials(true);
                config.setAllowedOrigins(Arrays.asList(frontendUrl));
                config.setAllowedHeaders(Arrays.asList(
                                "Origin", "Content-Type", "Accept", "Authorization", "X-Requested-With"));
                config.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "OPTIONS", "DELETE", "PATCH"));
                config.setExposedHeaders(List.of("Authorization"));
                config.setMaxAge(3600L);
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}

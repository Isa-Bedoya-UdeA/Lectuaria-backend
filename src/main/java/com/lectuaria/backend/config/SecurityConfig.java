package com.lectuaria.backend.config;

import com.lectuaria.backend.security.JwtAuthenticationEntryPoint;
import com.lectuaria.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          AuthenticationProvider authenticationProvider,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/library-books/template").permitAll()

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/*/reviews").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/*/ratings").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/search").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/popular").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/top-rated").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/isbn/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/genre/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/genres").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/author/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/featured").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/*/similar").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/*").permitAll()

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/libraries/me/statistics").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/*/reading-statistics").hasAnyRole("READER", "ADMIN")

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/libraries").permitAll()
                        .requestMatchers("/api/books/publish/**").permitAll()
                        .requestMatchers("/api/book-publish/**").permitAll()
                        .requestMatchers("/api/book-publish-test").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/library-books/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/library/**").permitAll()
                        .requestMatchers("/api/zones").permitAll()
                        .requestMatchers("/api/genres").permitAll()
                        .requestMatchers("/api/genres/with-count").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/*/reviews").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/*/stats").permitAll()

                        .requestMatchers("/api/library-books/**").authenticated()
                        .requestMatchers("/api/books/library/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/lists").permitAll()
                        .requestMatchers("/api/lists/**").authenticated()
                        .requestMatchers("/api/books/*/share").authenticated()
                        .requestMatchers("/api/books/*/share-link").authenticated()
                        .requestMatchers("/api/books/shares/**").authenticated()
                        .requestMatchers("/api/books/**").authenticated()

                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/notification-preferences/**").authenticated()

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/user-list-shares/public/**").permitAll()
                        .requestMatchers("/api/user-list-shares/**").authenticated()

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/friendships/search").permitAll()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

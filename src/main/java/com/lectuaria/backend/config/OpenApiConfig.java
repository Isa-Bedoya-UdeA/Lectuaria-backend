package com.lectuaria.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuracion de la documentacion OpenAPI / Swagger.
 *
 * Una vez la aplicacion este corriendo, la UI interactiva esta disponible en:
 *   http://&lt;host&gt;:&lt;port&gt;/swagger-ui.html
 * y el JSON OpenAPI en:
 *   http://&lt;host&gt;:&lt;port&gt;/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lectuariaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lectuaria API")
                        .description("API REST de Lectuaria, plataforma social-bibliotecaria " +
                                "de fomento a la lectura en Medellin. " +
                                "Los endpoints se sirven como recursos HATEOAS, lo que permite " +
                                "navegar hipermedia desde el cliente sin acoplamiento a URLs hardcodeadas.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo Lectuaria - Universidad de Antioquia")
                                .email("isabela.bedoya@udea.edu.co"))
                        .license(new License()
                                .name("Propietario")
                                .url("https://github.com/Isa-Bedoya-UdeA/Lectuaria-backend")))
                .servers(List.of(
                        new Server().url("http://localhost:3000").description("Servidor local"),
                        new Server().url("https://lectuaria-backend.onrender.com").description("Produccion")));
    }
}

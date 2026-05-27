package com.lectuaria.backend.service.auth.impl;

import com.lectuaria.backend.service.auth.IEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class ResendEmailServiceImpl implements IEmailService {

    private static final Logger logger = LoggerFactory.getLogger(ResendEmailServiceImpl.class);

    private final TemplateEngine templateEngine;

    @Value("${platform.name:Lectuaria}")
    private String platformName;

    @Value("${email.api.key}")
    private String apiKey;

    @Value("${email.from.address:noreply@resend.dev}")
    private String fromAddress;

    @Value("${email.from.name:Lectuaria}")
    private String fromName;

    private final HttpClient httpClient;

    public ResendEmailServiceImpl(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void sendRegistrationConfirmation(String to, String displayName) {
        logger.info("Registro: simulación de envío a {}", to);
    }

    @Override
    public void sendPasswordResetEmail(String to, String displayName, String resetUrl, int expiresInHours) {
        try {
            Context context = new Context();
            context.setVariables(Map.of(
                "appName", platformName,
                "name", displayName,
                "resetUrl", resetUrl,
                "expiresInHours", expiresInHours
            ));

            String htmlContent = templateEngine.process("password-reset", context);

            String jsonBody = String.format("""
                {
                    "from": "%s <%s>",
                    "to": ["%s"],
                    "subject": "Restablecer tu contraseña - %s",
                    "html": %s
                }
                """,
                fromName,
                fromAddress,
                to,
                platformName,
                escapeJson(htmlContent)
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Email de recuperación enviado exitosamente a: {}", to);
            } else {
                logger.error("Error al enviar email a {}. Status: {}, Response: {}",
                    to, response.statusCode(), response.body());
                throw new RuntimeException("Failed to send email: " + response.body());
            }

        } catch (Exception e) {
            logger.error("Error al enviar email de recuperación a: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
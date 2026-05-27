package com.lectuaria.backend.service.auth.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class EmailServiceImpl implements IEmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final TemplateEngine templateEngine;
    private final HttpClient httpClient;

    @Value("${platform.name:Lectuaria}")
    private String platformName;

    @Value("${email.api.key}")
    private String apiKey;

    @Value("${email.from.address}")
    private String fromAddress;

    @Value("${email.from.name:Lectuaria}")
    private String fromName;

    public EmailServiceImpl(TemplateEngine templateEngine) {
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

            // Build form-encoded body for Elastic Email v2 API
            String encoded = URLEncoder.encode(htmlContent, StandardCharsets.UTF_8);
            
            String body = String.format(
                "apikey=%s&from=%s&fromName=%s&msgTo=%s&subject=%s&bodyHtml=%s&isTransactional=true",
                URLEncoder.encode(apiKey, StandardCharsets.UTF_8),
                URLEncoder.encode(fromAddress, StandardCharsets.UTF_8),
                URLEncoder.encode(fromName, StandardCharsets.UTF_8),
                URLEncoder.encode(to, StandardCharsets.UTF_8),
                URLEncoder.encode("Restablecer tu contraseña - " + platformName, StandardCharsets.UTF_8),
                encoded
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.elasticemail.com/v2/email/send"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            logger.debug("Elastic Email API response: {} - {}", response.statusCode(), response.body());

            // Success is status 200 and response contains "success":true
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
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
}
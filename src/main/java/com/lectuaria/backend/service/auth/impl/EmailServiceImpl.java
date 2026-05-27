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
import java.util.Map;

@Service
public class EmailServiceImpl implements IEmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${platform.name:Lectuaria}")
    private String platformName;

    @Value("${email.api.key}")
    private String apiKey;

    @Value("${email.from.address:noreply@resend.dev}")
    private String fromAddress;

    @Value("${email.from.name:Lectuaria}")
    private String fromName;

    public EmailServiceImpl(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.objectMapper = new ObjectMapper();
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

            ObjectNode jsonBody = objectMapper.createObjectNode();
            jsonBody.put("from", fromName + " <" + fromAddress + ">");
            jsonBody.putPOJO("to", new String[]{to});
            jsonBody.put("subject", "Restablecer tu contraseña - " + platformName);
            jsonBody.put("html", htmlContent);

            String json = objectMapper.writeValueAsString(jsonBody);

            logger.debug("Email request JSON: {}", json);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            logger.debug("Resend API response: {} - {}", response.statusCode(), response.body());

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
}
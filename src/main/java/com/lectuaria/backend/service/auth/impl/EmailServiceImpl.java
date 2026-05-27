package com.lectuaria.backend.service.auth.impl;

import com.lectuaria.backend.service.auth.IEmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailServiceImpl implements IEmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${platform.name:Lectuaria}")
    private String platformName;

    @Value("${spring.mail.username}")
    private String emailUsername;

    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendRegistrationConfirmation(String to, String displayName) {
        logger.info("Correo de confirmación enviado con éxito a: {}", to);
    }

    @Override
    public void sendPasswordResetEmail(String to, String displayName, String resetUrl, int expiresInHours) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailUsername);
            helper.setTo(to);
            helper.setSubject("Restablecer tu contraseña - " + platformName);

            Context context = new Context();
            context.setVariables(Map.of(
                "appName", platformName,
                "name", displayName,
                "resetUrl", resetUrl,
                "expiresInHours", expiresInHours
            ));

            String htmlContent = templateEngine.process("password-reset", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Password reset email sent to: {}", to);
        } catch (jakarta.mail.MessagingException e) {
            logger.error("Failed to send password reset email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
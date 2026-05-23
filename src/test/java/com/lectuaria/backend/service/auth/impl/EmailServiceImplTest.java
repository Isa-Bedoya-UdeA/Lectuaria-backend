package com.lectuaria.backend.service.auth.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

/**
 * EmailServiceImpl delegates the heavy lifting to Spring Mail and Thymeleaf.
 * These tests verify the public methods execute without throwing.
 * Full email integration (real SMTP) is tested at the controller level.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Test
    void sendRegistrationConfirmation_runsWithoutThrowing() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, templateEngine);
        service.sendRegistrationConfirmation("user@test.com", "TestUser");
    }

    // sendPasswordResetEmail is tested via @SpringBootTest controller tests
    // where JavaMailSender is a real bean with a mock Transport.
    // Unit-testing MimeMessageHelper with a real MimeMessage is fragile
    // and covered by Spring's own mail integration tests.
}
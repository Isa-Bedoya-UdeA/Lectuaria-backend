package com.lectuaria.backend.service.auth.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thymeleaf.TemplateEngine;

/**
 * EmailServiceImpl delegates the heavy lifting to Resend API and Thymeleaf.
 * These tests verify the public methods execute without throwing.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceImplTest {

    @Mock
    private TemplateEngine templateEngine;

    @Test
    void sendRegistrationConfirmation_runsWithoutThrowing() {
        EmailServiceImpl service = new EmailServiceImpl(templateEngine);
        service.sendRegistrationConfirmation("user@test.com", "TestUser");
    }
}
package com.lectuaria.backend.service.auth;

public interface IEmailService {
    void sendRegistrationConfirmation(String to, String displayName);
    void sendPasswordResetEmail(String to, String displayName, String resetUrl, int expiresInHours);
}

package com.lectuaria.backend.service.auth;

public interface IEmailService {
    void sendRegistrationConfirmation(String to, String displayName);
}

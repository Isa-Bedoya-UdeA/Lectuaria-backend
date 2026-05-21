package com.lectuaria.backend.service.auth;

public interface IPasswordResetService {
    void requestPasswordReset(String email);
    void resetPassword(String token, String newPassword);
}
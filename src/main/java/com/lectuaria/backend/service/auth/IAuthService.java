package com.lectuaria.backend.service.auth;

import com.lectuaria.backend.dto.auth.LoginRequestDTO;
import com.lectuaria.backend.dto.auth.LoginResponseDTO;
import com.lectuaria.backend.dto.auth.ProfileResponseDTO;
import com.lectuaria.backend.dto.auth.ProfileUpdateRequestDTO;
import com.lectuaria.backend.dto.auth.RegisterRequestDTO;
import com.lectuaria.backend.dto.auth.RegisterResponseDTO;

public interface IAuthService {
    RegisterResponseDTO register(RegisterRequestDTO request);
    LoginResponseDTO login(LoginRequestDTO request, String ipAddress);
    void logout(String refreshTokenValue);
    LoginResponseDTO refresh(String refreshTokenValue);
    ProfileResponseDTO getProfile(String email);
    ProfileResponseDTO updateProfile(String email, ProfileUpdateRequestDTO request);
    void changePassword(String email, String currentPassword, String newPassword);
}

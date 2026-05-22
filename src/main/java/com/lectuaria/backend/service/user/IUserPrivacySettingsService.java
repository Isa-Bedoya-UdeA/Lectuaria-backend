package com.lectuaria.backend.service.user;

import com.lectuaria.backend.dto.user.UpdatePrivacySettingsRequestDTO;
import com.lectuaria.backend.dto.user.UserPrivacySettingsDTO;

public interface IUserPrivacySettingsService {
    UserPrivacySettingsDTO getSettings(Long userId);
    UserPrivacySettingsDTO updateSettings(Long userId, UpdatePrivacySettingsRequestDTO dto);
}
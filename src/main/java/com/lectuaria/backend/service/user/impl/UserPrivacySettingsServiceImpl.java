package com.lectuaria.backend.service.user.impl;

import com.lectuaria.backend.dto.user.UpdatePrivacySettingsRequestDTO;
import com.lectuaria.backend.dto.user.UserPrivacySettingsDTO;
import com.lectuaria.backend.model.user.UserPrivacySettings;
import com.lectuaria.backend.model.user.Visibility;
import com.lectuaria.backend.repository.user.UserPrivacySettingsRepository;
import com.lectuaria.backend.service.user.IUserPrivacySettingsService;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPrivacySettingsServiceImpl implements IUserPrivacySettingsService {

    private final UserPrivacySettingsRepository privacyRepository;
    private final UserRepository userRepository;

    public UserPrivacySettingsServiceImpl(UserPrivacySettingsRepository privacyRepository,
                                          UserRepository userRepository) {
        this.privacyRepository = privacyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserPrivacySettingsDTO getSettings(Long userId) {
        UserPrivacySettings settings = privacyRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        return toDTO(settings);
    }

    @Transactional
    public UserPrivacySettingsDTO updateSettings(Long userId, UpdatePrivacySettingsRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UserPrivacySettings settings = privacyRepository.findByUserId(userId)
                .orElseGet(() -> new UserPrivacySettings(user));

        settings.setProfileVisibility(dto.getProfileVisibility());
        settings.setReviewsVisibility(dto.getReviewsVisibility());
        settings.setReadingListsVisibility(dto.getReadingListsVisibility());
        settings.setReadingListsActivityVisibility(dto.getReadingListsActivityVisibility());
        settings.setFriendsVisibility(dto.getFriendsVisibility());

        settings = privacyRepository.save(settings);
        return toDTO(settings);
    }

    @Transactional
    public UserPrivacySettings getOrCreateSettings(Long userId) {
        return privacyRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
    }

    private UserPrivacySettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        UserPrivacySettings settings = new UserPrivacySettings(user);
        return privacyRepository.save(settings);
    }

    private UserPrivacySettingsDTO toDTO(UserPrivacySettings s) {
        return new UserPrivacySettingsDTO(
                s.getProfileVisibility(),
                s.getReviewsVisibility(),
                s.getReadingListsVisibility(),
                s.getReadingListsActivityVisibility(),
                s.getFriendsVisibility()
        );
    }
}
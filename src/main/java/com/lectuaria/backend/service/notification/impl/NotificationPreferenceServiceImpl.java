package com.lectuaria.backend.service.notification.impl;

import com.lectuaria.backend.dto.notification.NotificationPreferenceDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.notification.NotificationPreference;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.notification.NotificationPreferenceRepository;
import com.lectuaria.backend.service.notification.INotificationPreferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationPreferenceServiceImpl implements INotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public NotificationPreferenceServiceImpl(NotificationPreferenceRepository preferenceRepository, UserRepository userRepository) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<NotificationPreferenceDTO> getUserPreferences(Long userId) {
        List<NotificationPreference> preferences = preferenceRepository.findByUserId(userId);
        
        if (preferences.isEmpty()) {
            return createDefaultPreferences(userId);
        }
        
        return preferences.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationPreferenceDTO updatePreference(Long userId, NotificationType notificationType, boolean isEnabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        NotificationPreference preference = preferenceRepository.findByUserIdAndNotificationType(userId, notificationType)
                .orElse(new NotificationPreference(user, notificationType, true));

        preference.setEnabled(isEnabled);
        preference = preferenceRepository.save(preference);

        return mapToDTO(preference);
    }

    @Override
    @Transactional
    public void resetToDefaults(Long userId) {
        preferenceRepository.deleteByUserId(userId);
        createDefaultPreferences(userId);
    }

    private List<NotificationPreferenceDTO> createDefaultPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<NotificationPreference> defaultPreferences = Arrays.stream(NotificationType.values())
                .map(type -> new NotificationPreference(user, type, true))
                .collect(Collectors.toList());

        defaultPreferences = preferenceRepository.saveAll(defaultPreferences);

        return defaultPreferences.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private NotificationPreferenceDTO mapToDTO(NotificationPreference preference) {
        return new NotificationPreferenceDTO(
                preference.getId(),
                preference.getNotificationType(),
                preference.isEnabled()
        );
    }
}

package com.lectuaria.backend.repository.user;

import com.lectuaria.backend.model.user.UserPrivacySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPrivacySettingsRepository extends JpaRepository<UserPrivacySettings, Long> {
    Optional<UserPrivacySettings> findByUserId(Long userId);
}
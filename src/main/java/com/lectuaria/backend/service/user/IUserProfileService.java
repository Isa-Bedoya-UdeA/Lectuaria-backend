package com.lectuaria.backend.service.user;

import com.lectuaria.backend.dto.user.UserProfileDTO;
import com.lectuaria.backend.dto.user.UserStatsDTO;
import com.lectuaria.backend.model.auth.User;

public interface IUserProfileService {
    UserProfileDTO getUserProfileByUsername(String usernameSlug, User currentUser);
    UserStatsDTO getUserStats(String usernameSlug);
}

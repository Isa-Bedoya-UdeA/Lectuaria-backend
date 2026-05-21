package com.lectuaria.backend.service.user;

import com.lectuaria.backend.dto.user.FriendActivityDTO;
import com.lectuaria.backend.dto.user.UserProfileDTO;
import com.lectuaria.backend.dto.user.UserStatsDTO;
import com.lectuaria.backend.dto.statistics.ReadingStatisticsDTO;
import com.lectuaria.backend.dto.statistics.SocialStatisticsDTO;
import com.lectuaria.backend.model.auth.User;
import java.util.List;

public interface IUserProfileService {
    UserProfileDTO getUserProfileByUsername(String usernameSlug, User currentUser);
    UserStatsDTO getUserStats(String usernameSlug);
    ReadingStatisticsDTO getReadingStatistics(String usernameSlug);
    SocialStatisticsDTO getSocialStatistics(String usernameSlug);
    List<FriendActivityDTO> getFriendActivity(String usernameSlug, User currentUser);
}

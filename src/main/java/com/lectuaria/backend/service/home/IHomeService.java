package com.lectuaria.backend.service.home;

import com.lectuaria.backend.dto.home.FriendActivityDTO;
import com.lectuaria.backend.dto.home.HomeResponseDTO;
import com.lectuaria.backend.dto.recommendation.RecommendationDTO;
import com.lectuaria.backend.model.auth.User;
import java.util.List;

public interface IHomeService {
    HomeResponseDTO getHome(User user, Long genreId, String formatName);
    List<FriendActivityDTO> getFriendActivity(User user, int size);
    List<RecommendationDTO> getRecommendations(User user, int size);
    void hideRecommendation(User user, Long bookId);
}

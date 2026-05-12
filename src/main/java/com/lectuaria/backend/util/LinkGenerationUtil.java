package com.lectuaria.backend.util;

import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.repository.auth.UserRepository;

public class LinkGenerationUtil {

    private static final String BASE_URL = "http://localhost:3000";

    public static String generateBookShareLink(Long bookId) {
        return BASE_URL + "/books/" + bookId;
    }

    public static String generateReviewShareLink(Long reviewId) {
        return BASE_URL + "/reviews/" + reviewId;
    }

    public static String generateUserProfileLink(String usernameSlug, UserRepository userRepository) {
        String normalizedSlug = UsernameUtil.normalizeUsername(usernameSlug);
        User user = userRepository.findByUsername(normalizedSlug).orElse(null);
        if (user == null) {
            return null;
        }
        return BASE_URL + "/users/" + user.getUsername();
    }
}

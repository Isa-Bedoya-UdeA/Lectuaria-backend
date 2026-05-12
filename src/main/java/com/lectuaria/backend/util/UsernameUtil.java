package com.lectuaria.backend.util;

import com.lectuaria.backend.repository.auth.UserRepository;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class UsernameUtil {

    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-+");
    private static final Pattern LEADING_TRAILING_HYPHENS = Pattern.compile("^-|-$");

    public static String normalizeUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "";
        }

        String normalized = Normalizer.normalize(username, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "")
                .toLowerCase()
                .replaceAll("[_\\s]+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll(MULTIPLE_HYPHENS.pattern(), "-")
                .replaceAll(LEADING_TRAILING_HYPHENS.pattern(), "");

        return normalized.isEmpty() ? "user" : normalized;
    }

    public static String generateUniqueSlug(String username, UserRepository userRepository) {
        String baseSlug = normalizeUsername(username);
        String slug = baseSlug;
        int counter = 1;

        while (userRepository.existsByUsernameIgnoreCase(slug)) {
            slug = baseSlug + counter;
            counter++;
        }

        return slug;
    }
}

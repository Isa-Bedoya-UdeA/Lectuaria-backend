package com.lectuaria.backend.model.book;

public enum ReviewSortOption {
    MOST_RECENT,
    OLDEST,
    MOST_HELPFUL,
    HIGHEST_RATING,
    LOWEST_RATING;

    public static ReviewSortOption from(String value) {
        if (value == null || value.isBlank()) {
            return MOST_RECENT;
        }
        return switch (value.trim().toLowerCase()) {
            case "oldest", "mas_antiguas", "más antiguas" -> OLDEST;
            case "most_helpful", "mas_utiles", "más útiles" -> MOST_HELPFUL;
            case "highest_rating", "mayor_calificacion", "mayor calificación" -> HIGHEST_RATING;
            case "lowest_rating", "menor_calificacion", "menor calificación" -> LOWEST_RATING;
            default -> MOST_RECENT;
        };
    }
}

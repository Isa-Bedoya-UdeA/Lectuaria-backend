package com.lectuaria.backend.service.home.recommendation;

import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.repository.book.BookRatingRepository;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategy (GoF) que recomienda libros basandose en los generos que el
 * usuario ha calificado o agregado a sus listas. Es la primera estrategia
 * de la cadena (mayor prioridad) porque refleja directamente los gustos
 * explicitos del usuario.
 */
@Component
@Order(10)
public class PreferenceBasedRecommendationStrategy implements RecommendationStrategy {

    private final BookRatingRepository ratingRepository;
    private final UserListBookRepository listBookRepository;
    private final BookRepository bookRepository;

    public PreferenceBasedRecommendationStrategy(BookRatingRepository ratingRepository,
                                                  UserListBookRepository listBookRepository,
                                                  BookRepository bookRepository) {
        this.ratingRepository = ratingRepository;
        this.listBookRepository = listBookRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    public String name() {
        return "preference-based";
    }

    @Override
    public List<Book> selectCandidates(User user, int maxCandidates) {
        List<Object[]> genreRows = new ArrayList<>();
        genreRows.addAll(ratingRepository.findTopGenresByUserRatings(user.getId(), PageRequest.of(0, 10)));
        genreRows.addAll(listBookRepository.findTopGenresByUserLists(user.getId(), PageRequest.of(0, 10)));

        Map<Long, String> genreNames = genreRows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> (String) row[1],
                        (a, b) -> a,
                        java.util.LinkedHashMap::new));

        if (genreNames.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(bookRepository.findRecommendationsByGenreIds(
                new ArrayList<>(genreNames.keySet()),
                PageRequest.of(0, maxCandidates)));
    }

    @Override
    public String buildReason(Book book, Map<Long, String> preferredGenres) {
        if (book.getGenres() != null) {
            for (GenreDTO genre : book.getGenres().stream()
                    .map(g -> new GenreDTO(g.getId(), g.getName(), g.getDescription())).toList()) {
                if (preferredGenres.containsKey(genre.getId())) {
                    return "Sugerido porque has guardado o calificado libros de " + genre.getName();
                }
            }
        }
        return null;
    }

    @Override
    public BigDecimal score(Book book) {
        BigDecimal rating = book.getAverageRating() != null ? book.getAverageRating() : BigDecimal.ZERO;
        Integer count = book.getRatingsCount() != null ? book.getRatingsCount() : 0;
        return rating.add(BigDecimal.valueOf(Math.min(count, 1000) / 1000.0));
    }
}

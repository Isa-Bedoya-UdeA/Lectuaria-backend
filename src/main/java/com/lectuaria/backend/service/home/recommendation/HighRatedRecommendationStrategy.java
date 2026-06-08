package com.lectuaria.backend.service.home.recommendation;

import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.repository.book.BookRepository;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Strategy (GoF) de fallback: si el usuario no tiene historial de generos
 * o las recomendaciones basadas en preferencia no alcanzan el tamano pedido,
 * se recomiendan libros populares con rating alto (>= 4.0).
 */
@Component
@Order(20)
public class HighRatedRecommendationStrategy implements RecommendationStrategy {

    private final BookRepository bookRepository;

    public HighRatedRecommendationStrategy(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public String name() {
        return "high-rated-fallback";
    }

    @Override
    public List<Book> selectCandidates(User user, int maxCandidates) {
        return bookRepository.findFallbackRecommendations(PageRequest.of(0, maxCandidates));
    }

    @Override
    public String buildReason(Book book, Map<Long, String> preferredGenres) {
        if (book.getAverageRating() != null && book.getAverageRating().compareTo(BigDecimal.valueOf(4)) >= 0) {
            return "Sugerido por su alta calificacion en la comunidad";
        }
        return "Sugerido para descubrir nuevos libros populares";
    }

    @Override
    public BigDecimal score(Book book) {
        BigDecimal rating = book.getAverageRating() != null ? book.getAverageRating() : BigDecimal.ZERO;
        Integer count = book.getRatingsCount() != null ? book.getRatingsCount() : 0;
        return rating.add(BigDecimal.valueOf(Math.min(count, 1000) / 1000.0));
    }
}

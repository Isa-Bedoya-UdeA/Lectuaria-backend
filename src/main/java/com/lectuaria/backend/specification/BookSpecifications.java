package com.lectuaria.backend.specification;

import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Genre;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class BookSpecifications {

    public static Specification<Book> hasGenres(List<Long> genreIds) {
        return (root, query, cb) -> {
            if (genreIds == null || genreIds.isEmpty()) {
                return cb.conjunction();
            }
            Join<Book, Genre> genreJoin = root.join("genres");
            return genreJoin.get("id").in(genreIds);
        };
    }

    @SuppressWarnings("null")
    public static Specification<Book> containsKeywords(List<String> keywords) {
        return (root, query, cb) -> {
            query.distinct(true);

            if (keywords == null || keywords.isEmpty()) {
                return cb.conjunction();
            }

            Join<Book, Author> authorJoin = root.join("authors", JoinType.LEFT);
            Join<Book, Genre> genreJoin = root.join("genres", JoinType.LEFT);

            List<Predicate> keywordPredicates = new ArrayList<>();

            for (String keyword : keywords) {
                String likePattern = "%" + keyword.toLowerCase() + "%";

                Predicate titleMatch = cb.like(root.get("title"), likePattern);
                Predicate authorMatch = cb.like(authorJoin.get("name"), likePattern);
                Predicate genreMatch = cb.like(genreJoin.get("name"), likePattern);

                keywordPredicates.add(cb.or(titleMatch, authorMatch, genreMatch));
            }

            return cb.or(keywordPredicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Book> hasMinimumRating(Float minRating) {
        return (root, query, cb) -> {
            if (minRating == null || minRating <= 0) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("averageRating"), minRating);
        };
    }

    public static Specification<Book> inLibraries(List<Long> libraryIds) {
        return (root, query, cb) -> {
            if (libraryIds == null || libraryIds.isEmpty()) {
                return cb.conjunction();
            }
            query.distinct(true);
            Join<Object, Object> libraryJoin = root.join("libraryBooks");
            return libraryJoin.get("library").get("id").in(libraryIds);
        };
    }

    public static Specification<Book> hasMinPublicationYear(Integer minYear) {
        return (root, query, cb) -> {
            if (minYear == null) {
                return cb.conjunction();
            }
            Expression<Integer> yearExpression = cb.function("date_part", Integer.class,
                cb.literal("year"), root.get("publicationDate"));
            return cb.and(
                    cb.isNotNull(root.get("publicationDate")),
                    cb.greaterThanOrEqualTo(yearExpression, minYear));
        };
    }

    public static Specification<Book> hasMaxPublicationYear(Integer maxYear) {
        return (root, query, cb) -> {
            if (maxYear == null) {
                return cb.conjunction();
            }
            Expression<Integer> yearExpression = cb.function("date_part", Integer.class,
                cb.literal("year"), root.get("publicationDate"));
            return cb.and(
                    cb.isNotNull(root.get("publicationDate")),
                    cb.lessThanOrEqualTo(yearExpression, maxYear));
        };
    }

    public static Specification<Book> hasFormatTypes(List<String> formatTypes) {
        return (root, query, cb) -> {
            if (formatTypes == null || formatTypes.isEmpty()) {
                return cb.conjunction();
            }
            query.distinct(true);
            Join<Book, Object> libraryBookJoin = root.join("libraryBooks", JoinType.INNER);
            
            List<Predicate> formatPredicates = new ArrayList<>();
            for (String formatType : formatTypes) {
                if ("physical".equals(formatType)) {
                    formatPredicates.add(cb.greaterThan(libraryBookJoin.get("physicalCopies"), 0));
                } else if ("digital".equals(formatType)) {
                    formatPredicates.add(cb.equal(libraryBookJoin.get("digitalAvailable"), true));
                }
            }
            
            return cb.or(formatPredicates.toArray(new Predicate[0]));
        };
    }
}
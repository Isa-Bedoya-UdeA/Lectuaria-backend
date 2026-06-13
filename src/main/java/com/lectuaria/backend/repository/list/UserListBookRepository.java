package com.lectuaria.backend.repository.list;

import com.lectuaria.backend.model.list.UserListBook;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserListBookRepository extends JpaRepository<UserListBook, Long> {

    List<UserListBook> findByUserListId(Long listId);
    List<UserListBook> findByUserListIdOrderByAddedAtDesc(Long listId);

    @Query("SELECT ulb FROM UserListBook ulb WHERE ulb.userList.user.id IN :userIds ORDER BY ulb.addedAt DESC")
    List<UserListBook> findRecentByUserIds(@Param("userIds") List<Long> userIds, Pageable pageable);

    @Query("SELECT ulb.book FROM UserListBook ulb WHERE ulb.userList.name = :listName AND ulb.addedAt >= :since GROUP BY ulb.book ORDER BY COUNT(ulb) DESC, MAX(ulb.addedAt) DESC")
    List<com.lectuaria.backend.model.book.Book> findMostAddedToListSince(@Param("listName") String listName, @Param("since") Instant since, Pageable pageable);

    /**
     * Variante relajada de {@link #findMostAddedToListSince(String, Instant, Pageable)} para
     * alimentar la seccion "Mas leidos este mes" del Home.
     *
     * Senales de "leido" que se cuentan en el periodo (cualquiera de las dos):
     *  - El libro esta en una lista cuyo nombre cae en {@code listNames}
     *    (Leidos, Favoritos, y sus variantes sin tilde y en femenino).
     *  - El libro tiene al menos una calificacion en {@code BookRating}
     *    creada en el periodo.
     *
     * {@code since} es OBLIGATORIO y no-null (el caller pasa el inicio del
     * mes actual). Esto evita el problema conocido de PostgreSQL con
     * {@code :param IS NULL} en subqueries (PG no puede inferir el tipo
     * del parámetro y lanza "could not determine data type of parameter $N").
     *
     * El score de ranking es: (#agregadosALaLista) + (#calificaciones),
     * ambas restringidas al periodo {@code [since, now)}.
     */
    @Query(value = """
            SELECT b FROM com.lectuaria.backend.model.book.Book b
            WHERE b.id IN (
                SELECT ulb.book.id FROM UserListBook ulb
                WHERE ulb.userList.name IN :listNames
                  AND ulb.addedAt >= :since
                GROUP BY ulb.book.id
            )
            OR b.id IN (
                SELECT br.book.id FROM com.lectuaria.backend.model.book.BookRating br
                WHERE br.createdAt >= :since
                GROUP BY br.book.id
            )
            ORDER BY (
                (SELECT COUNT(ulb2) FROM UserListBook ulb2
                 WHERE ulb2.book = b AND ulb2.userList.name IN :listNames
                   AND ulb2.addedAt >= :since)
                +
                (SELECT COUNT(br2) FROM com.lectuaria.backend.model.book.BookRating br2
                 WHERE br2.book = b
                   AND br2.createdAt >= :since)
            ) DESC
            """)
    List<com.lectuaria.backend.model.book.Book> findMostReadSignals(
            @Param("listNames") List<String> listNames,
            @Param("since") Instant since,
            Pageable pageable);

    Optional<UserListBook> findByUserListIdAndBookId(Long listId, Long bookId);

    /** Find which of the user's lists contains this book */
    @Query("SELECT ulb FROM UserListBook ulb WHERE ulb.userList.user.id = :userId AND ulb.book.id = :bookId")
    List<UserListBook> findByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Modifying
    @Query("DELETE FROM UserListBook ulb WHERE ulb.userList.user.id = :userId AND ulb.book.id = :bookId")
    void deleteByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Query("SELECT COUNT(ulb) FROM UserListBook ulb WHERE ulb.userList.id = :listId")
    long countByListId(@Param("listId") Long listId);
    long countByUserListId(Long listId);

    @Modifying
    @Query("DELETE FROM UserListBook ulb WHERE ulb.userList.id = :listId")
    void deleteByUserListId(@Param("listId") Long listId);

    long countDistinctByUserListUserId(Long userId);

    @Query("SELECT g.id, g.name, COUNT(ulb) FROM UserListBook ulb JOIN ulb.book.genres g " +
            "WHERE ulb.userList.user.id = :userId GROUP BY g.id, g.name ORDER BY COUNT(ulb) DESC, g.name ASC")
    List<Object[]> findTopGenresByUserLists(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT DISTINCT ulb.book.id FROM UserListBook ulb WHERE ulb.userList.user.id = :userId")
    List<Long> findBookIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT ulb.book.id FROM UserListBook ulb WHERE ulb.userList.user.id = :userId " +
            "AND LOWER(ulb.userList.name) IN ('favoritos', 'leídos', 'leidos')")
    List<Long> findReadBookIdsInListsByUserId(@Param("userId") Long userId);

    @Query("SELECT ulb.book.id, ulb.addedAt FROM UserListBook ulb WHERE ulb.userList.user.id = :userId " +
            "AND LOWER(ulb.userList.name) IN ('favoritos', 'leídos', 'leidos')")
    List<Object[]> findReadBooksAndAddedAtByUserId(@Param("userId") Long userId);
}

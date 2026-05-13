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
}

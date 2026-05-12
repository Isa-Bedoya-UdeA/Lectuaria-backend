package com.lectuaria.backend.repository.friendship;

import com.lectuaria.backend.model.friendship.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE (f.user1.id = :userId OR f.user2.id = :userId)")
    List<Friendship> findFriendsByUserId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE (f.user1.id = :user1Id AND f.user2.id = :user2Id) OR (f.user1.id = :user2Id AND f.user2.id = :user1Id)")
    boolean existsByUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT f FROM Friendship f WHERE (f.user1.id = :user1Id AND f.user2.id = :user2Id) OR (f.user1.id = :user2Id AND f.user2.id = :user1Id)")
    Optional<Friendship> findByUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}

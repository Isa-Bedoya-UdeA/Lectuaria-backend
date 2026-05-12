package com.lectuaria.backend.repository.friendship;

import com.lectuaria.backend.model.friendship.FriendshipRequest;
import com.lectuaria.backend.model.friendship.FriendshipRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRequestRepository extends JpaRepository<FriendshipRequest, Long> {

    List<FriendshipRequest> findByReceiverIdAndStatus(Long receiverId, FriendshipRequestStatus status);

    List<FriendshipRequest> findBySenderIdAndStatus(Long senderId, FriendshipRequestStatus status);

    Optional<FriendshipRequest> findBySenderIdAndReceiverId(Long senderId, Long receiverId);

    @Query("SELECT CASE WHEN COUNT(fr) > 0 THEN true ELSE false END FROM FriendshipRequest fr WHERE ((fr.sender.id = :user1 AND fr.receiver.id = :user2) OR (fr.sender.id = :user2 AND fr.receiver.id = :user1)) AND fr.status = 'pending'")
    boolean hasPendingRequestBetween(@Param("user1") Long user1, @Param("user2") Long user2);

    @Query("SELECT fr FROM FriendshipRequest fr WHERE ((fr.sender.id = :user1 AND fr.receiver.id = :user2) OR (fr.sender.id = :user2 AND fr.receiver.id = :user1)) AND fr.status = 'pending'")
    Optional<FriendshipRequest> findPendingRequestBetween(@Param("user1") Long user1, @Param("user2") Long user2);

    /** Deletes ALL requests between two users regardless of status — used when a friendship is removed. */
    @Modifying
    @Query("DELETE FROM FriendshipRequest fr WHERE (fr.sender.id = :user1 AND fr.receiver.id = :user2) OR (fr.sender.id = :user2 AND fr.receiver.id = :user1)")
    void deleteAllRequestsBetween(@Param("user1") Long user1, @Param("user2") Long user2);
}

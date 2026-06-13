package com.lectuaria.backend.repository.list;

import com.lectuaria.backend.model.list.UserListShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserListShareRepository extends JpaRepository<UserListShare, Long> {

    List<UserListShare> findByListId(Long listId);

    List<UserListShare> findByReceiverIdAndIsActiveTrue(Long receiverId);

    List<UserListShare> findByOwnerIdAndIsActiveTrue(Long ownerId);

    Optional<UserListShare> findByListIdAndReceiverId(Long listId, Long receiverId);

    Optional<UserListShare> findByListIdAndReceiverIdAndIsActiveTrue(Long listId, Long receiverId);

    Optional<UserListShare> findByShareTokenAndIsActiveTrue(String shareToken);

    void deleteByListId(Long listId);

    /**
     * Desactiva en bloque todos los shares activos de una lista (no los borra).
     * Se usa cuando una lista pasa a PRIVATE para que los destinatarios no
     * puedan seguir accediendo al enlace.
     */
    @Modifying
    @Query("UPDATE UserListShare s SET s.isActive = false WHERE s.list.id = :listId AND s.isActive = true")
    int deactivateAllByListId(@Param("listId") Long listId);
}

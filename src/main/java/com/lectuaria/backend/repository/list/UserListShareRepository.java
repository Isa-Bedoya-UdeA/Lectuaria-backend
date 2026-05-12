package com.lectuaria.backend.repository.list;

import com.lectuaria.backend.model.list.UserListShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserListShareRepository extends JpaRepository<UserListShare, Long> {

    List<UserListShare> findByListId(Long listId);

    List<UserListShare> findByReceiverIdAndIsActiveTrue(Long receiverId);

    List<UserListShare> findByOwnerIdAndIsActiveTrue(Long ownerId);

    Optional<UserListShare> findByListIdAndReceiverId(Long listId, Long receiverId);

    void deleteByListId(Long listId);
}

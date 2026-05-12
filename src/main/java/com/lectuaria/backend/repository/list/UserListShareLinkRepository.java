package com.lectuaria.backend.repository.list;

import com.lectuaria.backend.model.list.UserListShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserListShareLinkRepository extends JpaRepository<UserListShareLink, Long> {

    Optional<UserListShareLink> findByListId(Long listId);

    List<UserListShareLink> findAllByListId(Long listId);

    Optional<UserListShareLink> findByPublicTokenAndIsActiveTrue(String publicToken);

    void deleteByListId(Long listId);
}

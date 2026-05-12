package com.lectuaria.backend.repository.list;

import com.lectuaria.backend.model.list.UserList;
import com.lectuaria.backend.model.list.ListType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserListRepository extends JpaRepository<UserList, Long> {

    List<UserList> findByUserIdOrderByCreatedAtAsc(Long userId);

    @Query("SELECT ul FROM UserList ul WHERE ul.user.id = :userId AND ul.name = :name")
    Optional<UserList> findByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);

    Optional<UserList> findByUserIdAndNameAndListType(Long userId, String name, ListType listType);

    @Query("SELECT COUNT(ul) FROM UserList ul WHERE ul.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /** Only public lists or lists owned by the requesting user */
    @Query("SELECT ul FROM UserList ul WHERE ul.id = :listId AND (ul.visibility = 'PUBLIC' OR ul.user.id = :requesterId)")
    Optional<UserList> findAccessible(@Param("listId") Long listId, @Param("requesterId") Long requesterId);
}

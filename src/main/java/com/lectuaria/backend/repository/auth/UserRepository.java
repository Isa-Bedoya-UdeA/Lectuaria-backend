package com.lectuaria.backend.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lectuaria.backend.model.auth.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndEmailNot(String username, String email);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.role = :role AND u.id <> :userId AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')))")
    java.util.List<User> searchReaders(@org.springframework.data.repository.query.Param("query") String query,
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("role") com.lectuaria.backend.model.auth.UserRole role);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.role = :role AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')))")
    java.util.List<User> searchReadersPublic(@org.springframework.data.repository.query.Param("query") String query,
            @org.springframework.data.repository.query.Param("role") com.lectuaria.backend.model.auth.UserRole role);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

}
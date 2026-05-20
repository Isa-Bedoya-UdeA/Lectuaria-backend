package com.lectuaria.backend.repository.book;

import com.lectuaria.backend.model.book.UserRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRecommendationRepository extends JpaRepository<UserRecommendation, Long> {

    @Query("SELECT ur FROM UserRecommendation ur WHERE ur.user.id = :userId AND ur.hidden = false")
    List<UserRecommendation> findActiveRecommendationsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT ur FROM UserRecommendation ur WHERE ur.user.id = :userId AND ur.book.id = :bookId")
    Optional<UserRecommendation> findByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);
    
    @Modifying
    @Query("DELETE FROM UserRecommendation ur WHERE ur.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserRecommendation ur WHERE ur.user.id = :userId AND ur.hidden = false")
    void deleteActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT ur.book.id FROM UserRecommendation ur WHERE ur.user.id = :userId AND ur.hidden = true")
    List<Long> findHiddenBookIdsByUserId(@Param("userId") Long userId);

    List<UserRecommendation> findByUserIdAndHiddenFalse(Long userId);
}

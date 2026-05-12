package com.lectuaria.backend.repository.book;

import com.lectuaria.backend.model.book.BookShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookShareRepository extends JpaRepository<BookShare, Long> {

    List<BookShare> findByReceiverId(Long receiverId);

    List<BookShare> findBySenderId(Long senderId);

    @Query("SELECT CASE WHEN COUNT(bs) > 0 THEN true ELSE false END FROM BookShare bs WHERE bs.sender.id = :senderId AND bs.receiver.id = :receiverId AND bs.book.id = :bookId")
    boolean existsBySenderAndReceiverAndBook(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId, @Param("bookId") Long bookId);

    Optional<BookShare> findBySenderIdAndReceiverIdAndBookId(Long senderId, Long receiverId, Long bookId);
}

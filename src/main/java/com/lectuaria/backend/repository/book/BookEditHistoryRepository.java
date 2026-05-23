package com.lectuaria.backend.repository.book;

import com.lectuaria.backend.model.book.BookEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookEditHistoryRepository extends JpaRepository<BookEditHistory, Long> {
}
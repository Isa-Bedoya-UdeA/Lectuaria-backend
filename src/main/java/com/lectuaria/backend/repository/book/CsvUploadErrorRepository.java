package com.lectuaria.backend.repository.book;

import com.lectuaria.backend.model.book.CsvUploadError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsvUploadErrorRepository extends JpaRepository<CsvUploadError, Long> {
}

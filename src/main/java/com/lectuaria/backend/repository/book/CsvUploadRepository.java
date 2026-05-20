package com.lectuaria.backend.repository.book;

import com.lectuaria.backend.model.book.CsvUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsvUploadRepository extends JpaRepository<CsvUpload, Long> {
}

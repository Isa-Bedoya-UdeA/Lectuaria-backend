package com.lectuaria.backend.service.book;

import com.lectuaria.backend.dto.book.BulkUploadResultDTO;
import org.springframework.web.multipart.MultipartFile;

public interface IBulkUploadService {
    BulkUploadResultDTO processCsv(MultipartFile file, Long userId);
}

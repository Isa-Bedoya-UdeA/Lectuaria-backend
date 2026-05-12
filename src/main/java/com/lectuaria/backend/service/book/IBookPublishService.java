package com.lectuaria.backend.service.book;

import com.lectuaria.backend.dto.book.BookPublishRequestDTO;
import com.lectuaria.backend.dto.book.BookPublishResponseDTO;
import org.springframework.lang.NonNull;

public interface IBookPublishService {
    BookPublishResponseDTO publishBook(BookPublishRequestDTO request, Long librarianUserId);
    BookPublishRequestDTO prefillFromOpenLibrary(@NonNull Long isbn, Long librarianUserId);
}

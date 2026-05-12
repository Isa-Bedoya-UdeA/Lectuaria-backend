package com.lectuaria.backend.service.book.externalApi;

import com.lectuaria.backend.dto.book.externalApi.OpenLibraryBookDTO;
import org.springframework.lang.NonNull;

public interface IOpenLibraryService {
    OpenLibraryBookDTO fetchBookByIsbn(@NonNull Long isbn);
}

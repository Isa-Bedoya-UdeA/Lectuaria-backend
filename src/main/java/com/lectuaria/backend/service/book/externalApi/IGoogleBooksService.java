package com.lectuaria.backend.service.book.externalApi;

import com.lectuaria.backend.dto.book.externalApi.GoogleBooksVolumeDTO;
import org.springframework.lang.NonNull;

public interface IGoogleBooksService {
    GoogleBooksVolumeDTO fetchBookByIsbn(@NonNull Long isbn);
}

package com.lectuaria.backend.service.book.externalApi;

import com.lectuaria.backend.dto.book.externalApi.ExternalBookMetadataDTO;
import org.springframework.lang.NonNull;

public interface IExternalBookMetadataService {
    ExternalBookMetadataDTO fetchBookMetadata(@NonNull Long isbn);
}

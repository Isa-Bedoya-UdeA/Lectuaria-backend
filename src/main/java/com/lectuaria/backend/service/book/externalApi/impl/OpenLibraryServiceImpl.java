package com.lectuaria.backend.service.book.externalApi.impl;

import com.lectuaria.backend.dto.book.externalApi.OpenLibraryBookDTO;
import com.lectuaria.backend.service.book.externalApi.IOpenLibraryService;
import com.lectuaria.backend.exception.OpenLibraryApiException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

@Service
public class OpenLibraryServiceImpl implements IOpenLibraryService {

    private static final String OPEN_LIBRARY_BASE_URL = "https://openlibrary.org/api/books?bibkeys=ISBN:{isbn}&jscmd=data&format=json";

    private final RestTemplate restTemplate;

    public OpenLibraryServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    public OpenLibraryBookDTO fetchBookByIsbn(@NonNull Long isbn) {
        try {
            String url = OPEN_LIBRARY_BASE_URL.replace("{isbn}", isbn.toString());

            ResponseEntity<Map<String, OpenLibraryBookDTO>> response = restTemplate.exchange(
                    Objects.requireNonNull(url),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, OpenLibraryBookDTO>>() {
                    });

            Map<String, OpenLibraryBookDTO> body = response.getBody();

            if (body == null || body.isEmpty()) {
                throw new OpenLibraryApiException("No se encontró el libro en OpenLibrary con ISBN: " + isbn);
            }

            String key = "ISBN:" + isbn;
            OpenLibraryBookDTO book = body.get(key);

            if (book == null) {
                throw new OpenLibraryApiException("No se encontró el libro en OpenLibrary con ISBN: " + isbn);
            }

            return book;
        } catch (RestClientException e) {
            throw new OpenLibraryApiException("Error al conectar con OpenLibrary: " + e.getMessage());
        }
    }
}
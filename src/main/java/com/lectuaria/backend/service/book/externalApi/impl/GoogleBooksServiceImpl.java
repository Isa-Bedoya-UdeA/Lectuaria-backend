package com.lectuaria.backend.service.book.externalApi.impl;

import com.lectuaria.backend.dto.book.externalApi.GoogleBooksResponseDTO;
import com.lectuaria.backend.service.book.externalApi.IGoogleBooksService;
import com.lectuaria.backend.dto.book.externalApi.GoogleBooksVolumeDTO;
import com.lectuaria.backend.exception.GoogleBooksApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GoogleBooksServiceImpl implements IGoogleBooksService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleBooksServiceImpl.class);

    private static final String GOOGLE_BOOKS_URL = "https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}&langRestrict=es";

    private final RestTemplate restTemplate;

    public GoogleBooksServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    public GoogleBooksVolumeDTO fetchBookByIsbn(@NonNull Long isbn) {

        try {

            String url = GOOGLE_BOOKS_URL.replace("{isbn}", isbn.toString());
            logger.info("Buscando en Google Books API con URL: {}", url);

            ResponseEntity<GoogleBooksResponseDTO> response = restTemplate.getForEntity(Objects.requireNonNull(url),
                    GoogleBooksResponseDTO.class);

            GoogleBooksResponseDTO body = response.getBody();
            logger.info("Respuesta de Google Books API - Status: {}, TotalItems: {}",
                    response.getStatusCode(), body != null ? body.getTotalItems() : "null");

            if (body == null || body.getTotalItems() == 0 || body.getItems() == null || body.getItems().isEmpty()) {
                throw new GoogleBooksApiException("No se encontró el libro en Google Books con ISBN: " + isbn);
            }

            GoogleBooksVolumeDTO volume = body.getItems().get(0);
            logger.info("Libro encontrado - Title: {}, Authors: {}, HasImageLinks: {}",
                    volume.getVolumeInfo() != null ? volume.getVolumeInfo().getTitle() : "null",
                    volume.getVolumeInfo() != null ? volume.getVolumeInfo().getAuthors() : "null",
                    volume.getVolumeInfo() != null && volume.getVolumeInfo().getImageLinks() != null ? "YES" : "NO");

            return volume;

        } catch (RestClientException e) {
            logger.error("Error al conectar con Google Books API: {}", e.getMessage(), e);
            throw new GoogleBooksApiException("Error al conectar con Google Books API: " + e.getMessage(), e);
        }

    }

}
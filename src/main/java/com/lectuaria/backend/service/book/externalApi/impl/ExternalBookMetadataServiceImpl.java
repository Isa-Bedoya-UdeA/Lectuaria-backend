package com.lectuaria.backend.service.book.externalApi.impl;

import com.lectuaria.backend.dto.book.externalApi.ExternalBookMetadataDTO;
import com.lectuaria.backend.dto.book.externalApi.GoogleBooksIndustryIdentifierDTO;
import com.lectuaria.backend.dto.book.externalApi.GoogleBooksVolumeDTO;
import com.lectuaria.backend.dto.book.externalApi.GoogleBooksVolumeInfoDTO;
import com.lectuaria.backend.dto.book.externalApi.OpenLibraryBookDTO;
import com.lectuaria.backend.exception.GoogleBooksApiException;
import com.lectuaria.backend.exception.OpenLibraryApiException;
import com.lectuaria.backend.service.book.externalApi.IExternalBookMetadataService;
import com.lectuaria.backend.service.book.externalApi.IGoogleBooksService;
import com.lectuaria.backend.service.book.externalApi.IOpenLibraryService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExternalBookMetadataServiceImpl implements IExternalBookMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalBookMetadataServiceImpl.class);

    private final IGoogleBooksService googleBooksService;
    private final IOpenLibraryService openLibraryService;

    public ExternalBookMetadataServiceImpl(IGoogleBooksService googleBooksService,
            IOpenLibraryService openLibraryService) {
        this.googleBooksService = googleBooksService;
        this.openLibraryService = openLibraryService;
    }

    public ExternalBookMetadataDTO fetchBookMetadata(@NonNull Long isbn) {

        ExternalBookMetadataDTO metadata = new ExternalBookMetadataDTO();

        GoogleBooksVolumeDTO googleBook = null;
        OpenLibraryBookDTO openLibraryBook = null;

        // Intentar Google Books
        try {
            googleBook = googleBooksService.fetchBookByIsbn(isbn);
        } catch (GoogleBooksApiException ignored) {
        }

        // Intentar OpenLibrary
        try {
            openLibraryBook = openLibraryService.fetchBookByIsbn(isbn);
        } catch (OpenLibraryApiException ignored) {
        }

        if (googleBook == null && openLibraryBook == null) {
            return null;
        }

        // GOOGLE BOOKS PRIORIDAD SIEMPRE + OPENLIBRARY COMPLEMENTARIO
        if (googleBook != null && googleBook.getVolumeInfo() != null) {

            GoogleBooksVolumeInfoDTO info = googleBook.getVolumeInfo();

            // Google Books tiene prioridad absoluta para título, autores, editorial, fecha,
            // páginas, descripción
            metadata.setTitle(info.getTitle());
            metadata.setAuthors(info.getAuthors());
            metadata.setPublisher(info.getPublisher());
            metadata.setPublishedDate(info.getPublishedDate());
            metadata.setPageCount(info.getPageCount());

            // FILTRAR DESCRIPCIÓN - solo español o null
            if (info.getDescription() != null) {
                String description = info.getDescription();
                if (description != null && description.toLowerCase().contains("english description")) {
                    // Si contiene "english description", extraer solo la parte en español
                    String[] parts = description.split("ENGLISH DESCRIPTION");
                    if (parts.length > 0) {
                        String spanishPart = parts[0].trim();
                        if (!spanishPart.isEmpty()) {
                            metadata.setDescription(spanishPart);
                            LOGGER.info("Google Books - Descripción filtrada (solo español): {}",
                                    spanishPart.substring(0, Math.min(100, spanishPart.length())) + "...");
                        }
                    }
                } else {
                    // Si no tiene "english description", no usar descripción (mejor manual)
                    LOGGER.info("Google Books - Descripción no filtrada, dejando null para ingreso manual");
                    metadata.setDescription(null);
                }
            }

            LOGGER.info("Google Books - Title encontrado: {}, Description: {}, Cover: {}",
                    info.getTitle(),
                    info.getDescription(),
                    info.getImageLinks() != null ? info.getImageLinks().getThumbnail() : "NO IMAGE");

            if (info.getIndustryIdentifiers() != null) {
                for (GoogleBooksIndustryIdentifierDTO id : info.getIndustryIdentifiers()) {

                    if ("ISBN_13".equals(id.getType())) {
                        metadata.setIsbn13(id.getIdentifier());
                    }

                    if ("ISBN_10".equals(id.getType())) {
                        metadata.setIsbn10(id.getIdentifier());
                    }
                }
            }
        }

        // OPENLIBRARY COMPLEMENTARIO (mejorar datos de Google Books)
        if (openLibraryBook != null) {

            // Complementar solo si Google Books no tiene el dato específico
            if (metadata.getTitle() == null) {
                metadata.setTitle(openLibraryBook.getTitle());
                LOGGER.info("OpenLibrary - Título complementario: {}", openLibraryBook.getTitle());
            }

            if (metadata.getAuthors() == null && openLibraryBook.getAuthors() != null) {
                List<String> authors = openLibraryBook.getAuthors()
                        .stream()
                        .map(OpenLibraryBookDTO.NameObject::getName)
                        .collect(Collectors.toList());

                metadata.setAuthors(authors);
                LOGGER.info("OpenLibrary - Autores complementarios: {}", authors);
            }

            if (metadata.getPublisher() == null && openLibraryBook.getPublishers() != null) {
                metadata.setPublisher(openLibraryBook.getPublishers().get(0).getName());
                LOGGER.info("OpenLibrary - Editorial complementaria: {}",
                        openLibraryBook.getPublishers().get(0).getName());
            }

            if (metadata.getPublishedDate() == null) {
                metadata.setPublishedDate(openLibraryBook.getPublishDate());
                LOGGER.info("OpenLibrary - Fecha complementaria: {}", openLibraryBook.getPublishDate());
            }

            if (metadata.getPageCount() == null) {
                metadata.setPageCount(openLibraryBook.getNumberOfPages());
                LOGGER.info("OpenLibrary - Páginas complementarias: {}", openLibraryBook.getNumberOfPages());
            }

            // OpenLibrary tiene prioridad ABSOLUTA para portadas (mejor calidad)
            if (openLibraryBook.getCover() != null && openLibraryBook.getCover().getLarge() != null) {
                String coverUrl = openLibraryBook.getCover().getLarge();
                // Ignorar URLs con ID -1 (indica que no se encontró portada)
                if (!coverUrl.contains("/id/-1-")) {
                    metadata.setCoverUrl(coverUrl);
                    LOGGER.info("OpenLibrary - Cover URL prioritaria: {}", coverUrl);
                } else {
                    LOGGER.info("OpenLibrary - Cover URL inválida (ID -1), ignorando");
                }
            } else {
                LOGGER.info("OpenLibrary - No tiene portada disponible");
            }
        }

        // Google Books fallback para portada (solo si OpenLibrary no tiene portada)
        if (metadata.getCoverUrl() == null && googleBook != null && googleBook.getVolumeInfo() != null) {
            GoogleBooksVolumeInfoDTO info = googleBook.getVolumeInfo();
            if (info.getImageLinks() != null) {
                metadata.setCoverUrl(info.getImageLinks().getThumbnail());
                LOGGER.info("Google Books - Cover URL fallback: {}", info.getImageLinks().getThumbnail());
            }
        }

        LOGGER.info("Metadata final - Title: {}, Authors: {}, CoverUrl: {}, Publisher: {}, Description: {}",
                metadata.getTitle(),
                metadata.getAuthors(),
                metadata.getCoverUrl(),
                metadata.getPublisher(),
                metadata.getDescription());

        return metadata;
    }
}
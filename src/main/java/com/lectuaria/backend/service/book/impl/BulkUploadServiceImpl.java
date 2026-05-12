package com.lectuaria.backend.service.book.impl;

import com.lectuaria.backend.dto.book.*;
import com.lectuaria.backend.service.book.IBulkUploadService;
import com.lectuaria.backend.service.book.IBookPublishService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service to process bulk book uploads via CSV.
 * Rewritten to use standard Java libraries only to avoid external dependency
 * issues.
 */
@Service
public class BulkUploadServiceImpl implements IBulkUploadService {

    private final IBookPublishService bookPublishService;

    public BulkUploadServiceImpl(IBookPublishService bookPublishService) {
        this.bookPublishService = bookPublishService;
    }

    public BulkUploadResultDTO processCsv(MultipartFile file, Long userId) {
        BulkUploadResultDTO result = new BulkUploadResultDTO();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String lineContent;
            int rowIndex = 1;

            // Skip header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                result.addError("El archivo está vacío");
                return result;
            }

            rowIndex++;

            while ((lineContent = reader.readLine()) != null) {
                if (lineContent.trim().isEmpty())
                    continue;

                try {
                    // Simple CSV parsing (handles quotes basicly)
                    String[] line = parseCsvLine(lineContent);

                    // Structure: ISBN, Title, Authors, Genres, Description, Editorial, Pages, PublicationDate, CoverUrl, PhysicalCopies, DigitalAvailable
                    BookPublishRequestDTO request = new BookPublishRequestDTO();

                    try {
                        request.setIsbn(Long.parseLong(line[0].trim()));
                    } catch (NumberFormatException e) {
                        result.addError("Fila " + rowIndex + ": ISBN inválido (" + line[0] + ")");
                        continue;
                    }

                    request.setTitle(line[1].trim());
                    request.setAuthors(Arrays.asList(line[2].split(";")).stream().map(String::trim)
                            .filter(s -> !s.isEmpty()).toList());
                    request.setGenres(Arrays.asList(line[3].split(";")).stream().map(String::trim)
                            .filter(s -> !s.isEmpty()).toList());
                    
                    // New fields
                    if (line.length > 4 && !line[4].trim().isEmpty()) {
                        request.setDescription(line[4].trim());
                    } else {
                        request.setDescription("Sin descripción");
                    }

                    if (line.length > 5 && !line[5].trim().isEmpty()) {
                        request.setPublishers(Arrays.asList(line[5].split(";")).stream().map(String::trim)
                                .filter(s -> !s.isEmpty()).toList());
                    } else {
                        request.setPublishers(new ArrayList<>());
                    }

                    if (line.length > 6 && !line[6].trim().isEmpty()) {
                        try {
                            request.setPages(Integer.parseInt(line[6].trim()));
                        } catch (NumberFormatException e) {
                            // Ignore invalid page numbers
                        }
                    }

                    if (line.length > 7 && !line[7].trim().isEmpty()) {
                        try {
                            request.setPublicationDate(java.time.LocalDate.parse(line[7].trim()));
                        } catch (Exception e) {
                            // Ignore invalid dates
                        }
                    }

                    if (line.length > 8 && !line[8].trim().isEmpty()) {
                        request.setCoverUrl(line[8].trim());
                    }

                    AvailabilityDTO availability = new AvailabilityDTO();
                    String formatoRaw = line.length > 9 ? line[9].trim().toLowerCase() : "fisico";
                    // Normalizar: quitar acentos de manera simple para comparaciones comunes
                    String formato = formatoRaw.replace("í", "i").replace("á", "a").replace("é", "e").replace("ó", "o").replace("ú", "u");
                    
                    int physicalCount = 0;
                    
                    if (line.length > 10 && !line[10].trim().isEmpty()) {
                        try {
                            physicalCount = Integer.parseInt(line[10].trim());
                        } catch (NumberFormatException e) {
                            // Default a 0
                        }
                    }

                    if (formato.equals("digital")) {
                        availability.setPhysical(false);
                        availability.setPhysicalCopies(0);
                        availability.setDigital(true);
                    } else if (formato.equals("ambos")) {
                        availability.setPhysical(true);
                        availability.setPhysicalCopies(physicalCount > 0 ? physicalCount : 1);
                        availability.setDigital(true);
                    } else { // default a físico
                        availability.setPhysical(true);
                        availability.setPhysicalCopies(physicalCount > 0 ? physicalCount : 1);
                        availability.setDigital(false);
                    }
                    
                    request.setAvailability(availability);
                    request.setLibraryId(userId); // Ensure we use the userId's library

                    BookPublishResponseDTO response = bookPublishService.publishBook(request, userId);
                    result.addSuccess(response);

                } catch (Exception e) {
                    result.addError("Fila " + rowIndex + ": " + e.getMessage());
                }
                rowIndex++;
            }
        } catch (Exception e) {
            result.addError("Error general al procesar el archivo: " + e.getMessage());
        }

        return result;
    }

    /**
     * Minimal CSV line parser that handles commas inside quotes.
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder curVal = new StringBuilder();
        boolean inquotes = false;

        char[] chars = line.toCharArray();
        for (char ch : chars) {
            if (inquotes) {
                if (ch == '\"') {
                    inquotes = false;
                } else {
                    curVal.append(ch);
                }
            } else {
                if (ch == '\"') {
                    inquotes = true;
                } else if (ch == ',') {
                    result.add(curVal.toString());
                    curVal = new StringBuilder();
                } else {
                    curVal.append(ch);
                }
            }
        }
        result.add(curVal.toString());
        return result.toArray(new String[0]);
    }
}

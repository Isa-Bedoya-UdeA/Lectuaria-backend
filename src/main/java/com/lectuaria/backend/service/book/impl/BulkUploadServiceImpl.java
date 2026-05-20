package com.lectuaria.backend.service.book.impl;

import com.lectuaria.backend.dto.book.*;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.CsvUpload;
import com.lectuaria.backend.model.book.CsvUploadError;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.book.CsvUploadErrorRepository;
import com.lectuaria.backend.repository.book.CsvUploadRepository;
import com.lectuaria.backend.service.book.IBulkUploadService;
import com.lectuaria.backend.service.book.IBookPublishService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
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
    private final UserRepository userRepository;
    private final CsvUploadRepository csvUploadRepository;
    private final CsvUploadErrorRepository csvUploadErrorRepository;

    public BulkUploadServiceImpl(IBookPublishService bookPublishService,
                                 UserRepository userRepository,
                                 CsvUploadRepository csvUploadRepository,
                                 CsvUploadErrorRepository csvUploadErrorRepository) {
        this.bookPublishService = bookPublishService;
        this.userRepository = userRepository;
        this.csvUploadRepository = csvUploadRepository;
        this.csvUploadErrorRepository = csvUploadErrorRepository;
    }

    public BulkUploadResultDTO processCsv(MultipartFile file, Long userId) {
        BulkUploadResultDTO result = new BulkUploadResultDTO();

        // 1. Fetch user to link to upload log
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + userId));

        // 2. Initialize CsvUpload record
        CsvUpload csvUpload = new CsvUpload();
        csvUpload.setUser(user);
        csvUpload.setFileName(file.getOriginalFilename());
        csvUpload.setStatus("PENDING");
        csvUpload.setUploadDate(LocalDateTime.now());
        csvUpload = csvUploadRepository.save(csvUpload);

        int totalRecords = 0;
        int successfulRecords = 0;
        int failedRecords = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String lineContent;
            int rowIndex = 1;

            // Skip header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                result.addError("El archivo está vacío");
                csvUpload.setStatus("FAILED");
                csvUploadRepository.save(csvUpload);
                return result;
            }

            rowIndex++;

            while ((lineContent = reader.readLine()) != null) {
                if (lineContent.trim().isEmpty())
                    continue;

                totalRecords++;

                try {
                    // Simple CSV parsing (handles quotes basicly)
                    String[] line = parseCsvLine(lineContent);

                    // Structure: ISBN, Title, Authors, Genres, Description, Editorial, Pages, PublicationDate, CoverUrl, PhysicalCopies, DigitalAvailable
                    BookPublishRequestDTO request = new BookPublishRequestDTO();

                    Long isbn = null;
                    try {
                        isbn = Long.parseLong(line[0].trim());
                        request.setIsbn(isbn);
                    } catch (NumberFormatException e) {
                        String errMsg = "ISBN inválido (" + line[0] + ")";
                        result.addError("Fila " + rowIndex + ": " + errMsg);
                        saveUploadError(csvUpload, rowIndex, null, errMsg);
                        failedRecords++;
                        rowIndex++;
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
                    
                    // Validar que el formato sea uno de los permitidos
                    if (!formato.equals("digital") && !formato.equals("ambos") && !formato.equals("fisico")) {
                        String errMsg = "Formato inválido (\"" + formatoRaw + "\"). Los formatos permitidos son: físico, digital, ambos";
                        result.addError("Fila " + rowIndex + ": " + errMsg);
                        saveUploadError(csvUpload, rowIndex, isbn, errMsg);
                        failedRecords++;
                        rowIndex++;
                        continue;
                    }
                    
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
                    } else { // fisico
                        availability.setPhysical(true);
                        availability.setPhysicalCopies(physicalCount > 0 ? physicalCount : 1);
                        availability.setDigital(false);
                    }
                    
                    request.setAvailability(availability);
                    request.setLibraryId(userId); // Ensure we use the userId's library

                    BookPublishResponseDTO response = bookPublishService.publishBook(request, userId);
                    result.addSuccess(response);
                    successfulRecords++;

                } catch (Exception e) {
                    result.addError("Fila " + rowIndex + ": " + e.getMessage());
                    Long parsedIsbn = null;
                    try {
                        String[] line = parseCsvLine(lineContent);
                        parsedIsbn = Long.parseLong(line[0].trim());
                    } catch (Exception ignored) {}
                    saveUploadError(csvUpload, rowIndex, parsedIsbn, e.getMessage());
                    failedRecords++;
                }
                rowIndex++;
            }
        } catch (Exception e) {
            result.addError("Error general al procesar el archivo: " + e.getMessage());
            csvUpload.setStatus("FAILED");
        }

        // 3. Update CsvUpload stats and final status
        csvUpload.setTotalRecords(totalRecords);
        csvUpload.setSuccessfulRecords(successfulRecords);
        csvUpload.setFailedRecords(failedRecords);

        if (!"FAILED".equals(csvUpload.getStatus())) {
            if (successfulRecords > 0) {
                csvUpload.setStatus("PROCESSED");
            } else {
                csvUpload.setStatus("FAILED");
            }
        }
        csvUploadRepository.save(csvUpload);

        return result;
    }

    private void saveUploadError(CsvUpload csvUpload, int rowNumber, Long isbn, String message) {
        CsvUploadError errorRecord = new CsvUploadError();
        errorRecord.setCsvUpload(csvUpload);
        errorRecord.setRowNumber(rowNumber);
        errorRecord.setIsbn(isbn);
        errorRecord.setErrorMessage(message != null ? message : "Error desconocido");
        errorRecord.setCreatedAt(LocalDateTime.now());
        csvUploadErrorRepository.save(errorRecord);
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

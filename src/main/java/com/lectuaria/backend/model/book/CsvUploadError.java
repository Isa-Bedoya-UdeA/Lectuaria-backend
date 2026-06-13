package com.lectuaria.backend.model.book;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "csv_upload_error")
public class CsvUploadError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_error")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_upload", nullable = false)
    private CsvUpload csvUpload;

    @Column(name = "row_number_error", nullable = false)
    private Integer rowNumber;

    @Column(name = "isbn")
    private Long isbn;

    @Column(name = "error_message", nullable = false)
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructor sin argumentos requerido por JPA (Jakarta Persistence)
    // para instanciar la entidad via reflection al cargar desde la BD.
    public CsvUploadError() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CsvUpload getCsvUpload() {
        return csvUpload;
    }

    public void setCsvUpload(CsvUpload csvUpload) {
        this.csvUpload = csvUpload;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public Long getIsbn() {
        return isbn;
    }

    public void setIsbn(Long isbn) {
        this.isbn = isbn;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

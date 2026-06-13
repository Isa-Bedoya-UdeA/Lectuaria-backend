package com.lectuaria.backend.util;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for {@link EncodingHelper}. Verifies that CSV reading correctly
 * handles UTF-8 with/without BOM and Windows-1252 (default encoding used
 * by Windows Notepad), which is the case reported as broken in the
 * bulk-upload regression.
 */
class EncodingHelperTest {

    /**
     * Sample CSV line used across tests. Contains a tilde-accented genre
     * (Fantasía) to verify byte-vs-decoded content.
     */
    private static final String SAMPLE_CSV_LINE = "9780439139601,Harry Potter,J.K. Rowling,Fantasía";

    @Test
    void detectsUtf8WithBom() {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = SAMPLE_CSV_LINE.getBytes(StandardCharsets.UTF_8);
        byte[] full = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, full, 0, bom.length);
        System.arraycopy(content, 0, full, bom.length, content.length);

        Charset charset = EncodingHelper.detectCharset(full);
        assertEquals(StandardCharsets.UTF_8, charset);
    }

    @Test
    void detectsUtf8WithoutBom() {
        byte[] content = SAMPLE_CSV_LINE.getBytes(StandardCharsets.UTF_8);
        Charset charset = EncodingHelper.detectCharset(content);
        assertEquals(StandardCharsets.UTF_8, charset);
    }

    @Test
    void detectsWindows1252WhenNotUtf8() {
        // Encode the accented line in Windows-1252: 'á' = 0xE1 (single byte, invalid UTF-8 lead)
        byte[] content = SAMPLE_CSV_LINE.getBytes(Charset.forName("Windows-1252"));

        Charset charset = EncodingHelper.detectCharset(content);
        assertEquals(Charset.forName("Windows-1252"), charset);
    }

    @Test
    void readsUtf8WithBomStreamCorrectly() throws Exception {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = SAMPLE_CSV_LINE.getBytes(StandardCharsets.UTF_8);
        byte[] full = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, full, 0, bom.length);
        System.arraycopy(content, 0, full, bom.length, content.length);

        try (InputStream stream = new ByteArrayInputStream(full);
             BufferedReader reader = EncodingHelper.newCsvReader(stream)) {
            String line = reader.readLine();
            assertNotNull(line);
            assertEquals(SAMPLE_CSV_LINE, line);
        }
    }

    @Test
    void readsWindows1252StreamCorrectly() throws Exception {
        // The most important test: this is what was failing on production.
        // CSV from Windows Notepad arrives as Windows-1252 bytes; the reader
        // must decode it back to the original Spanish string.
        byte[] content = SAMPLE_CSV_LINE.getBytes(Charset.forName("Windows-1252"));

        try (InputStream stream = new ByteArrayInputStream(content);
             BufferedReader reader = EncodingHelper.newCsvReader(stream)) {
            String line = reader.readLine();
            assertNotNull(line);
            assertEquals(SAMPLE_CSV_LINE, line);
            // Crucially, the tilde must be preserved, not turned into 'Fantasía' -> 'FantasÃ­a'.
            assertEquals("Fantasía", extractGenre(line));
        }
    }

    @Test
    void readsUtf8WithoutBomStreamCorrectly() throws Exception {
        byte[] content = SAMPLE_CSV_LINE.getBytes(StandardCharsets.UTF_8);

        try (InputStream stream = new ByteArrayInputStream(content);
             BufferedReader reader = EncodingHelper.newCsvReader(stream)) {
            String line = reader.readLine();
            assertNotNull(line);
            assertEquals(SAMPLE_CSV_LINE, line);
        }
    }

    private static String extractGenre(String csvLine) {
        String[] parts = csvLine.split(",", -1);
        return parts[3];
    }
}

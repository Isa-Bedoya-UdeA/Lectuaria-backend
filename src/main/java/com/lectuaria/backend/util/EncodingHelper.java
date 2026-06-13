package com.lectuaria.backend.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Utilidad para leer archivos CSV detectando automáticamente el encoding.
 *
 * <p>La plataforma debe ser robusta frente a archivos creados en distintos
 * editores (Bloc de Notas, VSCode, Excel, etc.), que producen CSVs en
 * distintos encodings. Este helper:</p>
 * <ol>
 *   <li>Detecta UTF-8 con BOM (\uFEFF al inicio).</li>
 *   <li>Verifica si los bytes son UTF-8 bien-formado (sin secuencias
 *       truncadas, overlongs, ni surrogates).</li>
 *   <li>Cae a Windows-1252 (default histórico de Windows, usado por Bloc
 *       de Notas) cuando los bytes no son UTF-8 válido.</li>
 * </ol>
 *
 * <p><b>Implementación:</b> lee el stream completo a un buffer en memoria
 * y luego construye un {@link ByteArrayInputStream} sobre esos bytes. Esto
 * evita depender de {@code mark/reset} del stream original, que no todos
 * los {@link InputStream} retornados por frameworks (Spring Multipart,
 * servlets) soportan correctamente. El costo es cargar el CSV en RAM,
 * aceptable para los volúmenes esperados (bibliotecas con cientos de
 * libros a la vez, raramente >1 MB).</p>
 */
public final class EncodingHelper {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private EncodingHelper() {
        // utility class
    }

    /**
     * Construye un {@link BufferedReader} para un CSV detectando
     * automáticamente el encoding. El stream de entrada se consume por
     * completo.
     *
     * @param stream stream del archivo CSV.
     * @return reader con el charset detectado.
     */
    public static BufferedReader newCsvReader(InputStream stream) throws IOException {
        byte[] bytes = readAllBytes(stream);
        Charset charset = detectCharset(bytes);
        // Strip UTF-8 BOM (\uFEFF) if present; otherwise it would appear as
        // a leading character on the first line read by the consumer.
        if (charset == StandardCharsets.UTF_8 && bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(bytes, 3, bytes.length - 3), charset));
        }
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), charset));
    }

    /**
     * Lee el stream completo a un byte array.
     */
    private static byte[] readAllBytes(InputStream stream) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /**
     * Detecta el charset del byte array según el contenido:
     * <ol>
     *   <li>Si empieza con el BOM UTF-8 (EF BB BF) → UTF-8.</li>
     *   <li>Si los bytes son UTF-8 válidos → UTF-8.</li>
     *   <li>En otro caso → Windows-1252, que cubre todos los caracteres
     *       del español (tildes, ñ, ü) sin replacements.</li>
     * </ol>
     */
    public static Charset detectCharset(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }

        if (isLikelyUtf8(bytes)) {
            return StandardCharsets.UTF_8;
        }

        return Charset.forName("Windows-1252");
    }

    /**
     * Verifica si los bytes son UTF-8 bien-formado, sin bytes de
     * continuation sueltos, overlongs, ni surrogates.
     */
    private static boolean isLikelyUtf8(byte[] buffer) {
        int i = 0;
        int length = buffer.length;
        while (i < length) {
            int b = buffer[i] & 0xFF;
            if (b < 0x80) {
                i++;
                continue;
            }
            // 2-byte sequence: 110xxxxx 10xxxxxx
            if ((b & 0xE0) == 0xC0) {
                if (i + 1 >= length) return false;
                int b1 = buffer[i + 1] & 0xFF;
                if ((b1 & 0xC0) != 0x80) return false;
                if ((b & 0x1E) == 0) return false; // overlong
                i += 2;
                continue;
            }
            // 3-byte sequence: 1110xxxx 10xxxxxx 10xxxxxx
            if ((b & 0xF0) == 0xE0) {
                if (i + 2 >= length) return false;
                int b1 = buffer[i + 1] & 0xFF;
                int b2 = buffer[i + 2] & 0xFF;
                if ((b1 & 0xC0) != 0x80 || (b2 & 0xC0) != 0x80) return false;
                if ((b == 0xE0) && (b1 & 0xE0) == 0x80) return false; // overlong
                if ((b == 0xED) && (b1 & 0xE0) == 0xA0) return false; // surrogate
                i += 3;
                continue;
            }
            // 4-byte sequence: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
            if ((b & 0xF8) == 0xF0) {
                if (i + 3 >= length) return false;
                int b1 = buffer[i + 1] & 0xFF;
                int b2 = buffer[i + 2] & 0xFF;
                int b3 = buffer[i + 3] & 0xFF;
                if ((b1 & 0xC0) != 0x80 || (b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80) return false;
                if ((b == 0xF0) && (b1 & 0xF0) == 0x80) return false; // overlong
                i += 4;
                continue;
            }
            // 10xxxxxx suelto o 11111xxx no es UTF-8 válido
            return false;
        }
        return true;
    }
}

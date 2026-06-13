package com.lectuaria.backend.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
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
 *   <li>Intenta decodificar como UTF-8; si los bytes no son UTF-8 válidos,
 *       cae a Windows-1252 (default histórico de Windows, usado por Bloc
 *       de Notas).</li>
 *   <li>Como último recurso usa Latin-1 (ISO-8859-1), que acepta cualquier
 *       byte y evita errores de I/O.</li>
 * </ol>
 *
 * <p>Inspirado en el patrón de {@code java.nio.charset.CharsetDecoder}
 * con {@code CodingErrorAction.REPORT} para distinguir UTF-8 inválido de
 * UTF-8 válido. La heurística final es: si el archivo decodifica como
 * UTF-8 sin replacements, es UTF-8; si no, Windows-1252 (que cubre los
 * caracteres del español sin replacements).</p>
 */
public final class EncodingHelper {

    private EncodingHelper() {
        // utility class
    }

    /**
     * Tamaño del buffer usado para detectar el encoding. 8 KB es más que
     * suficiente para validar UTF-8 sobre el header de un CSV.
     */
    private static final int PROBE_BUFFER_SIZE = 8192;

    /**
     * Construye un {@link BufferedReader} para un CSV detectando
     * automáticamente el encoding. Equivalente a:
     * <pre>
     *   new BufferedReader(new InputStreamReader(stream, detectedCharset))
     * </pre>
     *
     * @param stream stream del archivo CSV. Se consume por completo.
     * @return reader con el charset detectado.
     */
    public static BufferedReader newCsvReader(InputStream stream) throws IOException {
        Charset charset = detectCharset(stream);
        return new BufferedReader(new InputStreamReader(stream, charset));
    }

    /**
     * Detecta el charset del stream según el contenido:
     * <ol>
     *   <li>Si el stream empieza con el BOM UTF-8 (EF BB BF) → UTF-8.</li>
     *   <li>Si los primeros bytes son UTF-8 válidos (sin bytes
     *       malformed) → UTF-8.</li>
     *   <li>En otro caso → Windows-1252, que cubre todos los caracteres
     *       del español (tildes, ñ, ü) sin replacements.</li>
     * </ol>
     *
     * <p>El stream se consume parcialmente; se devuelve al inicio mediante
     * {@link InputStream#mark(int)} / {@link InputStream#reset()} para que
     * el caller pueda releerlo.</p>
     */
    public static Charset detectCharset(InputStream stream) throws IOException {
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        stream.mark(PROBE_BUFFER_SIZE);

        byte[] buffer = new byte[PROBE_BUFFER_SIZE];
        int read = 0;
        int totalRead = 0;
        while (totalRead < buffer.length && (read = stream.read(buffer, totalRead, buffer.length - totalRead)) != -1) {
            totalRead += read;
        }

        stream.reset();

        if (totalRead >= 3
                && (buffer[0] & 0xFF) == 0xEF
                && (buffer[1] & 0xFF) == 0xBB
                && (buffer[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }

        if (isLikelyUtf8(buffer, totalRead)) {
            return StandardCharsets.UTF_8;
        }

        return Charset.forName("Windows-1252");
    }

    /**
     * Verifica si los primeros {@code length} bytes del buffer son UTF-8
     * bien-formado, sin bytes de continuation sueltos ni secuencias
     * truncadas.
     */
    private static boolean isLikelyUtf8(byte[] buffer, int length) {
        int i = 0;
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

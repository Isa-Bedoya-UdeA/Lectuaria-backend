package com.lectuaria.backend.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
public class S3StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    @Value("${aws.s3.endpoint}")
    private String endpoint;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String s3SecretKey;

    // Extraído dinámicamente del endpoint para evitar hardcoding
    private String s3Host;

    private static final String SIGNING_ALGORITHM = "HmacSHA256";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final String SERVICE = "s3";
    private static final String REGION = "us-east-1";

    @PostConstruct
    public void init() {
        // Extraer el host del endpoint para usar en las firmas AWS Signature V4
        this.s3Host = extractHost(endpoint);
        logger.info("S3StorageService initialized for bucket: {} at endpoint: {}", bucket, endpoint);
        logger.debug("S3 Access Key: {}", accessKey);
        logger.debug("S3 Secret Key prefix: {}...", s3SecretKey.substring(0, Math.min(4, s3SecretKey.length())));
    }

    private String extractHost(String ep) {
        if (ep == null) return "unknown";
        try {
            return URI.create(ep).getHost();
        } catch (Exception e) {
            return ep.replace("https://", "").replace("http://", "").split("/")[0];
        }
    }

    public String uploadCoverBytes(byte[] imageBytes, long isbn, String contentType) throws IOException {
        String extension = getExtensionFromContentType(contentType);
        String key = isbn + "." + extension;

        // S3 virtual path used for AWS Signature V4
        String s3Path = "/storage/v1/s3";
        // The object path S3 expects
        String objectPath = s3Path + "/" + bucket + "/" + key;

        // Actual HTTP URL to PUT to — goes to S3, not public endpoint
        String urlStr = endpoint + "/" + bucket + "/" + key;
        URL url = URI.create(urlStr).toURL();
        String amzDate = getAmzDate();
        String payloadHash = "UNSIGNED-PAYLOAD";

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Host", s3Host);
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("x-amz-date", amzDate);
        conn.setRequestProperty("x-amz-content-sha256", payloadHash);

        String authHeader = computeSignatureV4("PUT", objectPath, "", s3Host, amzDate, payloadHash);
        conn.setRequestProperty("Authorization", authHeader);

        logger.debug("PUT {} - Host: {} - S3 Path: {} - Date: {}", urlStr, s3Host, objectPath, amzDate);
        logger.debug("Auth header: {}", authHeader);

        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);

        try {
            conn.connect();

            try (OutputStream os = conn.getOutputStream()) {
                os.write(imageBytes);
            }

            int status = conn.getResponseCode();
            if (status >= 200 && status < 300) {
                // Return the public Supabase Storage URL for browser access
                String publicUrl = endpoint.replace("/storage/v1/s3", "/storage/v1/object/public/") + bucket + "/" + key;
                logger.info("Uploaded cover for ISBN {} — public URL: {}", isbn, publicUrl);
                return publicUrl;
            } else {
                String error = readError(conn);
                throw new IOException("S3 PUT failed with status " + status + ": " + error);
            }
        } finally {
            conn.disconnect();
        }
    }

    public String uploadCover(MultipartFile file, long isbn) throws IOException {
        return uploadCoverBytes(file.getBytes(), isbn, file.getContentType());
    }

    public void deleteCover(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) return;

        try {
            // Extract key from public URL: /storage/v1/object/public/book-covers/1984898728.webp
            String objectKey = publicUrl.substring(publicUrl.lastIndexOf("/" + bucket + "/") + bucket.length() + 1);
            // S3 virtual path for signing
            String objectPath = "/storage/v1/s3/" + bucket + "/" + objectKey;

            String urlStr = endpoint + "/" + bucket + "/" + objectKey;
            URL url = URI.create(urlStr).toURL();
            String amzDate = getAmzDate();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Host", s3Host);
            conn.setRequestProperty("x-amz-date", amzDate);
            conn.setRequestProperty("x-amz-content-sha256", "UNSIGNED-PAYLOAD");

            String authHeader = computeSignatureV4("DELETE", objectPath, "", s3Host, amzDate, "UNSIGNED-PAYLOAD");
            conn.setRequestProperty("Authorization", authHeader);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);

            conn.connect();
            int status = conn.getResponseCode();
            conn.disconnect();

            if (status >= 200 && status < 300) {
                logger.info("Deleted cover: {}", publicUrl);
            } else {
                logger.warn("Failed to delete cover {}: HTTP {}", publicUrl, status);
            }
        } catch (Exception e) {
            logger.warn("Failed to delete cover {}: {}", publicUrl, e.getMessage());
        }
    }

    public String extractIsbnFromFileName(String fileName) {
        if (fileName == null) return null;
        String nameWithoutExt = fileName;
        int dotIdx = nameWithoutExt.lastIndexOf('.');
        if (dotIdx > 0) {
            nameWithoutExt = nameWithoutExt.substring(0, dotIdx);
        }
        StringBuilder sb = new StringBuilder();
        for (char c : nameWithoutExt.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
        }
        return sb.toString();
    }

    // ─── AWS Signature V4 ─────────────────────────────────────────────────────

    private String computeSignatureV4(String method, String path, String query,
                                      String host, String amzDate, String payloadHash) throws IOException {
        String dateShort = amzDate.substring(0, 8);

        // Canonical request
        String canonicalRequest = method + "\n" +
            path + "\n" +
            query + "\n" +
            "host:" + host + "\n" +
            "x-amz-content-sha256:" + payloadHash + "\n" +
            "x-amz-date:" + amzDate + "\n" +
            "\n" +
            "host;x-amz-content-sha256;x-amz-date\n" +
            payloadHash;

        String canonicalHash = sha256Hex(canonicalRequest);

        String stringToSign = "AWS4-HMAC-SHA256\n" +
            amzDate + "\n" +
            dateShort + "/" + REGION + "/" + SERVICE + "/aws4_request\n" +
            canonicalHash;

        logger.debug("Canonical request:\n{}", canonicalRequest);
        logger.debug("String to sign:\n{}", stringToSign);

        byte[] kSecret = ("AWS4" + s3SecretKey).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateShort.getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = hmacSha256(kDate, REGION.getBytes(StandardCharsets.UTF_8));
        byte[] kService = hmacSha256(kRegion, SERVICE.getBytes(StandardCharsets.UTF_8));
        byte[] kSigning = hmacSha256(kService, "aws4_request".getBytes(StandardCharsets.UTF_8));
        byte[] signature = hmacSha256(kSigning, stringToSign.getBytes(StandardCharsets.UTF_8));

        String sigHex = bytesToHex(signature);

        return "AWS4-HMAC-SHA256 " +
            "Credential=" + accessKey + "/" + dateShort + "/" + REGION + "/" + SERVICE + "/aws4_request, " +
            "SignedHeaders=host;x-amz-content-sha256;x-amz-date, " +
            "Signature=" + sigHex;
    }

    private String getAmzDate() {
        return java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    }

    private byte[] hmacSha256(byte[] key, byte[] data) throws IOException {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(SIGNING_ALGORITHM);
            mac.init(new javax.crypto.spec.SecretKeySpec(key, SIGNING_ALGORITHM));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IOException("HMAC-SHA256 failed", e);
        }
    }

    private String sha256Hex(String data) throws IOException {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return bytesToHex(md.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IOException("SHA-256 failed", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String readError(HttpURLConnection conn) {
        try {
            int code = conn.getResponseCode();
            StringBuilder sb = new StringBuilder();
            sb.append("HTTP ").append(code).append(" - ");
            try (var is = conn.getErrorStream()) {
                if (is != null) {
                    int ch;
                    while ((ch = is.read()) != -1) sb.append((char) ch);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) return "jpg";
        if (contentType.contains("png")) return "png";
        if (contentType.contains("webp")) return "webp";
        return "jpg";
    }
}
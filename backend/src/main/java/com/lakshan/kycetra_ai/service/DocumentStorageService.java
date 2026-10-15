
package com.lakshan.kycetra_ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private final Path storageDirectory;

    public DocumentStorageService(
            @Value("${kyc.storage.directory:./uploads/kyc}")
            String storageDirectory
    ) {
        this.storageDirectory = Path.of(storageDirectory)
                .toAbsolutePath()
                .normalize();
    }

    private void validateFileSignature(
            MultipartFile file,
            String contentType
    ) throws IOException {

        try (InputStream inputStream = file.getInputStream()) {
            byte[] bytes = inputStream.readNBytes(8);

            boolean valid = switch (contentType) {
                case "application/pdf" ->
                        bytes.length >= 5
                        && bytes[0] == '%'
                        && bytes[1] == 'P'
                        && bytes[2] == 'D'
                        && bytes[3] == 'F'
                        && bytes[4] == '-';

                case "image/jpeg" ->
                        bytes.length >= 3
                        && (bytes[0] & 0xFF) == 0xFF
                        && (bytes[1] & 0xFF) == 0xD8
                        && (bytes[2] & 0xFF) == 0xFF;

                case "image/png" ->
                        bytes.length >= 8
                        && (bytes[0] & 0xFF) == 0x89
                        && bytes[1] == 'P'
                        && bytes[2] == 'N'
                        && bytes[3] == 'G'
                        && (bytes[4] & 0xFF) == 0x0D
                        && (bytes[5] & 0xFF) == 0x0A
                        && (bytes[6] & 0xFF) == 0x1A
                        && (bytes[7] & 0xFF) == 0x0A;

                default -> false;
            };

            if (!valid) {
                throw new IllegalArgumentException(
                        "File content does not match the declared file type."
                );
            }
        }
    }

    public StoredFile store(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please upload a non-empty file."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size must not exceed 10 MB."
            );
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Only PDF, JPEG, and PNG files are supported."
            );
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Filename is required.");
        }

        // Validate actual file bytes before writing anything to disk.
        validateFileSignature(file, contentType);

        // Never use the client-supplied filename as the storage path.
        String storageKey = UUID.randomUUID().toString();

        Files.createDirectories(storageDirectory);

        Path destination = storageDirectory.resolve(storageKey).normalize();

        if (!destination.startsWith(storageDirectory)) {
            throw new IllegalStateException("Invalid storage path.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination);
        }

        return new StoredFile(
                storageKey,
                originalFilename,
                contentType,
                file.getSize()
        );
    }

    public void delete(String storageKey) throws IOException {

        Path file = storageDirectory.resolve(storageKey).normalize();

        if (!file.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid storage key.");
        }

        Files.deleteIfExists(file);
    }

    public record StoredFile(
            String storageKey,
            String originalFilename,
            String contentType,
            long fileSizeBytes
    ) {
    }
    
    public Path getStoredFilePath(String storageKey) throws IOException {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Invalid storage key.");
        }

        Path file = storageDirectory.resolve(storageKey).normalize();

        if (!file.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid storage key.");
        }

        if (!Files.isRegularFile(file)) {
            throw new IOException("Stored document file was not found.");
        }

        return file;
    }

}

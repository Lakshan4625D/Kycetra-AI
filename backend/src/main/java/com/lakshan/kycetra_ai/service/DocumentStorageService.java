
package com.lakshan.kycetra_ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    public StoredFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a non-empty file.");
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

        // Store under a generated name, never the supplied filename.
        String storageKey = UUID.randomUUID().toString();

        Files.createDirectories(storageDirectory);

        Path destination = storageDirectory.resolve(storageKey).normalize();

        if (!destination.startsWith(storageDirectory)) {
            throw new IllegalStateException("Invalid storage path.");
        }

        try (var inputStream = file.getInputStream()) {
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
}

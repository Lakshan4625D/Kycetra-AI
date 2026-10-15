
package com.lakshan.kycetra_ai.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DocumentStorageServiceTest {

    @TempDir
    Path tempDirectory;

    private DocumentStorageService createStorageService() {
        return new DocumentStorageService(
                tempDirectory.toString()
        );
    }

    @Test
    void validPdfSignatureIsAccepted() throws Exception {
        byte[] content = "%PDF-1.7\nsample content"
                .getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        MockMultipartFile file = new MockMultipartFile(
                "file", "passport.pdf", "application/pdf", content
        );

        DocumentStorageService storage = createStorageService();

        DocumentStorageService.StoredFile stored = storage.store(file);

        assertEquals("passport.pdf", stored.originalFilename());
        assertTrue(Files.exists(tempDirectory.resolve(stored.storageKey())));
    }

    @Test
    void fakePdfIsRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "this is not a PDF".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                )
        );

        DocumentStorageService storage = createStorageService();

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.store(file)
        );

        assertDirectoryIsEmpty();
    }

    @Test
    void invalidJpegSignatureIsRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.jpg",
                "image/jpeg",
                "not a jpeg".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> createStorageService().store(file)
        );

        assertDirectoryIsEmpty();
    }

    @Test
    void invalidPngSignatureIsRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "not a png".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> createStorageService().store(file)
        );

        assertDirectoryIsEmpty();
    }

    @Test
    void unsupportedContentTypeIsRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "hello".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> createStorageService().store(file)
        );

        assertDirectoryIsEmpty();
    }

    @Test
    void emptyFileIsRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> createStorageService().store(file)
        );

        assertDirectoryIsEmpty();
    }

    @Test
    void oversizedFileIsRejected() {
        byte[] content = new byte[10 * 1024 * 1024 + 1];

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                content
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> createStorageService().store(file)
        );

        assertDirectoryIsEmpty();
    }
    
    private void assertDirectoryIsEmpty() {
        if (Files.exists(tempDirectory)) {
            try (var files = Files.list(tempDirectory)) {
                assertEquals(0, files.count());
            } catch (java.io.IOException exception) {
                fail("Could not inspect storage directory", exception);
            }
        }
    }
}

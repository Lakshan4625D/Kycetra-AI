package com.lakshan.kycetra_ai.service;

import com.lakshan.kycetra_ai.entity.DocumentStatus;
import com.lakshan.kycetra_ai.entity.ExtractedDocument;
import com.lakshan.kycetra_ai.entity.KycDocument;
import com.lakshan.kycetra_ai.entity.User;
import com.lakshan.kycetra_ai.repository.ExtractedDocumentRepository;
import com.lakshan.kycetra_ai.repository.KycDocumentRepository;
import com.lakshan.kycetra_ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class KycDocumentProcessingServiceIntegrationTest {

    @Autowired
    private KycDocumentProcessingService processingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycDocumentRepository documentRepository;

    @Autowired
    private ExtractedDocumentRepository extractedDocumentRepository;

    @MockitoBean
    private DocumentStorageService storageService;

    @MockitoBean
    private PdfTextExtractionService extractionService;

    private User user1;
    private User user2;
    private KycDocument pdfDocument;

    @BeforeEach
    void setUp() {
        user1 = userRepository.saveAndFlush(
                createUser("processing_user_1")
        );
        user2 = userRepository.saveAndFlush(
                createUser("processing_user_2")
        );

        pdfDocument = documentRepository.saveAndFlush(
                createDocument(user1, "application/pdf")
        );
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("test-password-hash");
        user.setEnabled(true);
        return user;
    }

    private KycDocument createDocument(User owner, String contentType) {
        KycDocument document = new KycDocument();
        document.setUser(owner);
        document.setOriginalFilename(
                contentType.equals("application/pdf")
                        ? "identity.pdf"
                        : "identity.png"
        );
        document.setStorageKey("test-storage-key-" + UUID.randomUUID());
        document.setContentType(contentType);
        document.setFileSizeBytes(1024L);
        document.setStatus(DocumentStatus.UPLOADED);
        return document;
    }

    @Test
    void successfulProcessingStoresExtractedTextAndCompletesDocument()
            throws Exception {

        when(storageService.getStoredFilePath(pdfDocument.getStorageKey()))
                .thenReturn(Path.of("test.pdf"));

        when(extractionService.extractText(any(Path.class)))
                .thenReturn("Kycetra identity verification");

        ExtractedDocument result = processingService.processDocument(
                user1.getUsername(),
                pdfDocument.getId()
        );

        assertEquals(
                "Kycetra identity verification",
                result.getExtractedText()
        );
        assertEquals(
                pdfDocument.getId(),
                result.getDocument().getId()
        );

        KycDocument updated = documentRepository
                .findById(pdfDocument.getId())
                .orElseThrow();

        assertEquals(DocumentStatus.COMPLETED, updated.getStatus());
        assertEquals(
                1L,
                extractedDocumentRepository
                        .findByDocument_Id(pdfDocument.getId())
                        .stream()
                        .count()
        );
    }

    @Test
    void userCannotProcessAnotherUsersDocument() throws IOException {
        assertThrows(
                NoSuchElementException.class,
                () -> processingService.processDocument(
                        user2.getUsername(),
                        pdfDocument.getId()
                )
        );

        verify(storageService, never()).getStoredFilePath(anyString());
        verify(extractionService, never()).extractText(any(Path.class));
    }

    @Test
    void nonPdfDocumentIsRejected() throws IOException {
        KycDocument imageDocument = documentRepository.saveAndFlush(
                createDocument(user1, "image/png")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> processingService.processDocument(
                        user1.getUsername(),
                        imageDocument.getId()
                )
        );

        verify(storageService, never()).getStoredFilePath(anyString());
        verify(extractionService, never()).extractText(any(Path.class));
    }

    @Test
    void extractionFailureMarksDocumentAsFailed() throws Exception {
        when(storageService.getStoredFilePath(pdfDocument.getStorageKey()))
                .thenReturn(Path.of("broken.pdf"));

        when(extractionService.extractText(any(Path.class)))
                .thenThrow(new IOException("Invalid PDF"));

        assertThrows(
                IOException.class,
                () -> processingService.processDocument(
                        user1.getUsername(),
                        pdfDocument.getId()
                )
        );

        KycDocument updated = documentRepository
                .findById(pdfDocument.getId())
                .orElseThrow();

        assertEquals(DocumentStatus.FAILED, updated.getStatus());
        assertNotNull(updated.getFailureReason());
    }

    @Test
    void reprocessingUpdatesExistingExtractedText() throws Exception {
        when(storageService.getStoredFilePath(pdfDocument.getStorageKey()))
                .thenReturn(Path.of("test.pdf"));

        when(extractionService.extractText(any(Path.class)))
                .thenReturn("Updated extracted text");

        ExtractedDocument existing = new ExtractedDocument();
        existing.setDocument(pdfDocument);
        existing.setExtractedText("Old extracted text");
        extractedDocumentRepository.saveAndFlush(existing);

        ExtractedDocument result = processingService.processDocument(
                user1.getUsername(),
                pdfDocument.getId()
        );

        assertEquals("Updated extracted text", result.getExtractedText());
        assertEquals(
                1L,
                extractedDocumentRepository
                        .findByDocument_Id(pdfDocument.getId())
                        .stream()
                        .count()
        );
    }
}
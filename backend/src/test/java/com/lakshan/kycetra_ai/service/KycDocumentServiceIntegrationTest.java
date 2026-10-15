
package com.lakshan.kycetra_ai.service;

import com.lakshan.kycetra_ai.dto.DocumentUploadResponse;
import com.lakshan.kycetra_ai.entity.KycDocument;
import com.lakshan.kycetra_ai.entity.User;
import com.lakshan.kycetra_ai.repository.KycDocumentRepository;
import com.lakshan.kycetra_ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class KycDocumentServiceIntegrationTest {

    @Autowired
    private KycDocumentService documentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycDocumentRepository documentRepository;

    @MockitoBean
    private DocumentStorageService storageService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        userRepository.deleteAll();

        user1 = createUser("integration_user_1");
        user2 = createUser("integration_user_2");

        user1 = userRepository.saveAndFlush(user1);
        user2 = userRepository.saveAndFlush(user2);
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("test-password-hash");
        user.setEnabled(true);
        return user;
    }

    private void mockStoredFile(String filename, String storageKey) throws Exception {
        when(storageService.store(any())).thenReturn(
                new DocumentStorageService.StoredFile(
                        storageKey,
                        filename,
                        "application/pdf",
                        1024L
                )
        );
    }

    private org.springframework.web.multipart.MultipartFile sampleFile() {
        return new MockMultipartFile(
                "file",
                "passport.pdf",
                "application/pdf",
                "sample content".getBytes()
        );
    }

    @Test
    void uploadPersistsDocumentWithCorrectOwner() throws Exception {
        mockStoredFile("passport.pdf", "integration-storage-key-1");

        DocumentUploadResponse response =
                documentService.upload(user1.getUsername(), sampleFile());

        assertNotNull(response.id());

        KycDocument saved = documentRepository.findById(response.id())
                .orElseThrow();

        assertEquals(user1.getId(), saved.getUser().getId());
        assertEquals("passport.pdf", saved.getOriginalFilename());
        assertEquals("application/pdf", saved.getContentType());
        assertEquals(1024L, saved.getFileSizeBytes());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void userCanRetrieveTheirOwnDocument() throws Exception {
        mockStoredFile("passport.pdf", "integration-storage-key-2");

        DocumentUploadResponse uploaded =
                documentService.upload(user1.getUsername(), sampleFile());

        DocumentUploadResponse retrieved =
                documentService.getMyDocument(
                        user1.getUsername(),
                        uploaded.id()
                );

        assertEquals(uploaded.id(), retrieved.id());
        assertEquals("passport.pdf", retrieved.filename());
    }

    @Test
    void userCannotRetrieveAnotherUsersDocument() throws Exception {
        mockStoredFile("passport.pdf", "integration-storage-key-3");

        DocumentUploadResponse uploaded =
                documentService.upload(user1.getUsername(), sampleFile());

        assertThrows(
                NoSuchElementException.class,
                () -> documentService.getMyDocument(
                        user2.getUsername(),
                        uploaded.id()
                )
        );
    }

    @Test
    void documentListingReturnsOnlyOwnersDocuments() throws Exception {
        mockStoredFile("passport.pdf", "integration-storage-key-4");

        documentService.upload(user1.getUsername(), sampleFile());

        mockStoredFile("identity-card.pdf", "integration-storage-key-5");

        documentService.upload(user2.getUsername(), sampleFile());

        List<DocumentUploadResponse> user1Documents =
                documentService.getMyDocuments(user1.getUsername());

        List<DocumentUploadResponse> user2Documents =
                documentService.getMyDocuments(user2.getUsername());

        assertEquals(1, user1Documents.size());
        assertEquals(1, user2Documents.size());

        assertEquals("passport.pdf", user1Documents.get(0).filename());
        assertEquals("identity-card.pdf", user2Documents.get(0).filename());
    }

    @Test
    void unknownUsernameCannotUploadDocument() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> documentService.upload(
                        "unknown_integration_user",
                        sampleFile()
                )
        );

        verify(storageService, never()).store(any());
    }

    @Test
    void userWithNoDocumentsGetsEmptyList() {
        List<DocumentUploadResponse> documents =
                documentService.getMyDocuments(user1.getUsername());

        assertNotNull(documents);
        assertTrue(documents.isEmpty());
    }

    @Test
    void unknownUsernameCannotListDocuments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> documentService.getMyDocuments(
                        "unknown_integration_user"
                )
        );
    }

    @Test
    void storageFailureDoesNotPersistDocument() throws Exception {
        when(storageService.store(any()))
                .thenThrow(new IOException("Simulated storage failure"));

        assertThrows(
                IOException.class,
                () -> documentService.upload(
                        user1.getUsername(),
                        sampleFile()
                )
        );

        assertTrue(documentRepository
                .findAllByUser_IdOrderByCreatedAtDesc(user1.getId())
                .isEmpty());
    }
}

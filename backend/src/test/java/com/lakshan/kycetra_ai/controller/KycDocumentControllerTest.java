package com.lakshan.kycetra_ai.controller;

import com.lakshan.kycetra_ai.dto.DocumentUploadResponse;
import com.lakshan.kycetra_ai.entity.DocumentStatus;
import com.lakshan.kycetra_ai.service.KycDocumentProcessingService;
import com.lakshan.kycetra_ai.service.KycDocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class KycDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KycDocumentService documentService;

    @MockitoBean
    private KycDocumentProcessingService processingService;

    private DocumentUploadResponse sampleDocument() {
        return new DocumentUploadResponse(
                1L,
                "passport.pdf",
                "application/pdf",
                1024L,
                DocumentStatus.UPLOADED,
                OffsetDateTime.parse("2026-10-14T10:00:00Z")
        );
    }

    private DocumentUploadResponse completedDocument() {
        return new DocumentUploadResponse(
                1L,
                "passport.pdf",
                "application/pdf",
                1024L,
                DocumentStatus.COMPLETED,
                OffsetDateTime.parse("2026-10-14T10:00:00Z")
        );
    }

    @Test
    @WithMockUser(username = "lakshan_test", roles = "USER")
    void uploadValidDocumentReturnsCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "passport.pdf",
                "application/pdf",
                "sample pdf content".getBytes()
        );

        when(documentService.upload(eq("lakshan_test"), any()))
                .thenReturn(sampleDocument());

        when(processingService.processDocument("lakshan_test", 1L))
                .thenReturn(null);

        when(documentService.getMyDocument("lakshan_test", 1L))
                .thenReturn(completedDocument());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.filename").value("passport.pdf"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(documentService).upload(eq("lakshan_test"), any());
        verify(processingService).processDocument("lakshan_test", 1L);
        verify(documentService).getMyDocument("lakshan_test", 1L);
    }

    @Test
    @WithMockUser(username = "lakshan_test", roles = "USER")
    void uploadEmptyFileReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        when(documentService.upload(eq("lakshan_test"), any()))
                .thenThrow(new IllegalArgumentException(
                        "Please upload a non-empty file."
                ));

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Please upload a non-empty file."));
    }

    @Test
    @WithMockUser(username = "lakshan_test", roles = "USER")
    void listMyDocumentsReturnsDocuments() throws Exception {
        when(documentService.getMyDocuments("lakshan_test"))
                .thenReturn(List.of(sampleDocument()));

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].filename").value("passport.pdf"));

        verify(documentService).getMyDocuments("lakshan_test");
    }

    @Test
    @WithMockUser(username = "lakshan_test", roles = "USER")
    void getMyDocumentReturnsDocument() throws Exception {
        when(documentService.getMyDocument("lakshan_test", 1L))
                .thenReturn(sampleDocument());

        mockMvc.perform(get("/api/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.filename").value("passport.pdf"));
    }

    @Test
    @WithMockUser(username = "lakshan_test", roles = "USER")
    void cannotRetrieveAnotherUsersDocument() throws Exception {
        when(documentService.getMyDocument("lakshan_test", 99L))
                .thenThrow(new NoSuchElementException("Document not found."));

        mockMvc.perform(get("/api/documents/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Document not found."));
    }

    @Test
    void unauthenticatedUserCannotListDocuments() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedUserCannotUploadDocuments() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "passport.pdf",
                "application/pdf",
                "sample pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }
}
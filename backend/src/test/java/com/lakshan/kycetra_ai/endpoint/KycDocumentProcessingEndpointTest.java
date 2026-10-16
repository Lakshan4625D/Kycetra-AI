package com.lakshan.kycetra_ai.endpoint;

import com.lakshan.kycetra_ai.entity.DocumentStatus;
import com.lakshan.kycetra_ai.entity.ExtractedDocument;
import com.lakshan.kycetra_ai.entity.KycDocument;
import com.lakshan.kycetra_ai.service.KycDocumentProcessingService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class KycDocumentProcessingEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KycDocumentProcessingService processingService;

    @Test
    void authenticatedUserCanProcessDocument() throws Exception {
        KycDocument document = mock(KycDocument.class);
        ExtractedDocument extracted = mock(ExtractedDocument.class);

        when(document.getId()).thenReturn(10L);
        when(document.getStatus()).thenReturn(DocumentStatus.COMPLETED);
        when(extracted.getDocument()).thenReturn(document);
        when(extracted.getCharacterCount()).thenReturn(250);
        when(extracted.getUpdatedAt()).thenReturn(OffsetDateTime.now());

        when(processingService.processDocument("analyst", 10L))
                .thenReturn(extracted);

        mockMvc.perform(post("/api/documents/10/process")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(10))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.characterCount").value(250))
                .andExpect(jsonPath("$.processedAt").exists());

        verify(processingService).processDocument("analyst", 10L);
    }

    @Test
    void missingOrUnownedDocumentReturnsNotFound() throws Exception {
        when(processingService.processDocument("analyst", 999L))
                .thenThrow(new NoSuchElementException("Document not found."));

        mockMvc.perform(post("/api/documents/999/process")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Document not found."));
    }

    @Test
    void invalidDocumentRequestReturnsBadRequest() throws Exception {
        when(processingService.processDocument("analyst", 10L))
                .thenThrow(new IllegalArgumentException(
                        "Only PDF documents can be processed currently."));

        mockMvc.perform(post("/api/documents/10/process")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isBadRequest());
    }
}
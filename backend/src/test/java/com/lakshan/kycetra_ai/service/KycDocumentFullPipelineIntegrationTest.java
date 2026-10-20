package com.lakshan.kycetra_ai.service;

import com.lakshan.kycetra_ai.entity.DocumentChunk;
import com.lakshan.kycetra_ai.entity.ExtractedDocument;
import com.lakshan.kycetra_ai.entity.KycDocument;
import com.lakshan.kycetra_ai.entity.User;
import com.lakshan.kycetra_ai.repository.DocumentChunkRepository;
import com.lakshan.kycetra_ai.repository.ExtractedDocumentRepository;
import com.lakshan.kycetra_ai.repository.KycDocumentRepository;
import com.lakshan.kycetra_ai.repository.UserRepository;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "kyc.storage.directory=target/test-kyc-uploads",
        "kycetra.chunking.chunk-size=300",
        "kycetra.chunking.chunk-overlap=50"
})
@AutoConfigureMockMvc
@Transactional
class KycDocumentFullPipelineIntegrationTest {

    private static final String MARKER = "KYCETRA_PIPELINE_MARKER";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycDocumentRepository documentRepository;

    @Autowired
    private ExtractedDocumentRepository extractedDocumentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private DocumentStorageService storageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    private String username;

    @BeforeEach
    void setUp() {
        username = "kyc_pipeline_" + UUID.randomUUID()
                .toString()
                .replace("-", "");

        User testUser = new User();
        testUser.setUsername(username);
        testUser.setEmail(username + "@example.com");
        testUser.setPasswordHash("integration-test-password-hash");
        testUser.setEnabled(true);

        userRepository.saveAndFlush(testUser);

        // Simulate Gemini embeddings without making external API calls.
        when(embeddingModel.embed(anyString())).thenAnswer(invocation -> {
            float[] vector = new float[768];
            vector[0] = 0.1f;
            return vector;
        });
    }

    @AfterEach
    void cleanUpStoredFiles() throws IOException {
        if (username == null) {
            return;
        }

        userRepository.findByUsername(username).ifPresent(testUser -> {
            documentRepository
                    .findAllByUser_IdOrderByCreatedAtDesc(testUser.getId())
                    .forEach(document -> {
                        try {
                            storageService.delete(document.getStorageKey());
                        } catch (IOException exception) {
                            throw new RuntimeException(
                                    "Could not clean up test PDF.",
                                    exception
                            );
                        }
                    });
        });
    }

    @Test
    void uploadingPdfExtractsChunksAndStoresEmbeddings() throws Exception {
        byte[] pdfBytes = createTextPdf();

        MockMultipartFile pdf = new MockMultipartFile(
                "file",
                "pipeline-test.pdf",
                "application/pdf",
                pdfBytes
        );

        MvcResult result = mockMvc.perform(
                        multipart("/api/documents/upload")
                                .file(pdf)
                                .with(user(username).roles("USER"))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename")
                        .value("pipeline-test.pdf"))
                .andExpect(jsonPath("$.contentType")
                        .value("application/pdf"))
                .andExpect(jsonPath("$.status")
                        .value("COMPLETED"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        Long documentId = Long.valueOf(
                com.jayway.jsonpath.JsonPath
                        .read(responseBody, "$.id")
                        .toString()
        );

        KycDocument storedDocument = documentRepository
                .findById(documentId)
                .orElseThrow();

        assertEquals(username, storedDocument.getUser().getUsername());
        assertEquals("COMPLETED", storedDocument.getStatus().name());

        // Verify that real PDF text extraction persisted the marker.
        ExtractedDocument extractedDocument =
                extractedDocumentRepository
                        .findByDocument_Id(documentId)
                        .orElseThrow();

        assertTrue(
                extractedDocument.getExtractedText().contains(MARKER),
                "The PDF marker should appear in the extracted text."
        );

        assertTrue(
                extractedDocument.getCharacterCount() > 0,
                "The extracted character count should be positive."
        );

        // Verify that chunking persisted multiple chunks.
        List<DocumentChunk> chunks =
                documentChunkRepository
                        .findAllByExtractedDocument_IdOrderByChunkIndexAsc(
                                extractedDocument.getId()
                        );

        assertTrue(
                chunks.size() > 1,
                "The PDF should be split into multiple chunks."
        );

        assertEquals(0, chunks.get(0).getChunkIndex());
        assertTrue(chunks.stream().allMatch(
                chunk -> chunk.getContent() != null
                        && !chunk.getContent().isBlank()
        ));

        // The embedding column is pgvector, so query it directly.
        Long embeddedChunkCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM document_chunks
                WHERE extracted_document_id = ?
                  AND embedding IS NOT NULL
                """,
                Long.class,
                extractedDocument.getId()
        );

        assertNotNull(embeddedChunkCount);
        assertEquals(
                (long) chunks.size(),
                embeddedChunkCount.longValue(),
                "Every stored chunk should have an embedding."
        );

        verify(embeddingModel, times(chunks.size()))
                .embed(anyString());
    }

    private byte[] createTextPdf() throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDType1Font font = new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA
            );

            int lineNumber = 0;

            // Three pages of real, extractable PDF text.
            // Enough text is generated to exercise chunk splitting.
            for (int pageNumber = 0; pageNumber < 3; pageNumber++) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream content =
                             new PDPageContentStream(document, page)) {

                    content.beginText();
                    content.setFont(font, 10);
                    content.newLineAtOffset(45, 750);

                    for (int line = 0; line < 20; line++) {
                        String text = String.format(
                                "%s Identity verification record %02d. "
                                        + "Customer: Ada Example. "
                                        + "Purpose: KYC compliance review.",
                                MARKER,
                                lineNumber++
                        );

                        content.showText(text);
                        content.newLineAtOffset(0, -30);
                    }

                    content.endText();
                }
            }

            document.save(output);
            return output.toByteArray();
        }
    }
}

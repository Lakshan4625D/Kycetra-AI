package com.lakshan.kycetra_ai.service;

import com.lakshan.kycetra_ai.entity.DocumentChunk;
import com.lakshan.kycetra_ai.entity.ExtractedDocument;
import com.lakshan.kycetra_ai.entity.KycDocument;
import com.lakshan.kycetra_ai.entity.User;
import com.lakshan.kycetra_ai.repository.DocumentChunkRepository;
import com.lakshan.kycetra_ai.repository.ExtractedDocumentRepository;
import com.lakshan.kycetra_ai.repository.KycDocumentRepository;
import com.lakshan.kycetra_ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class SemanticSearchIntegrationTest {

    @Autowired
    private SemanticSearchService searchService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycDocumentRepository documentRepository;

    @Autowired
    private ExtractedDocumentRepository extractedDocumentRepository;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = createUser("semantic_a");
        userB = createUser("semantic_b");

        // Deterministic query vector; no external Gemini call.
        when(embeddingModel.embed(anyString())).thenAnswer(invocation -> {
            float[] vector = new float[768];
            vector[0] = 1.0f;
            return vector;
        });
    }

    private User createUser(String prefix) {
        String username = prefix + "_" +
                UUID.randomUUID().toString().replace("-", "");

        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("integration-test-password-hash");
        user.setEnabled(true);

        return userRepository.saveAndFlush(user);
    }

    private DocumentChunk createChunk(
            User owner,
            String filename,
            int chunkIndex,
            String content,
            float firstVectorValue,
            boolean withEmbedding) {

        KycDocument document = new KycDocument();
        document.setUser(owner);
        document.setOriginalFilename(filename);
        document.setStorageKey(UUID.randomUUID().toString());
        document.setContentType("application/pdf");
        document.setFileSizeBytes(100L);
        document.setStatus(
                com.lakshan.kycetra_ai.entity.DocumentStatus.COMPLETED);

        document = documentRepository.saveAndFlush(document);

        ExtractedDocument extracted = new ExtractedDocument();
        extracted.setDocument(document);
        extracted.setExtractedText(content);
        extractedDocumentRepository.saveAndFlush(extracted);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setExtractedDocument(extracted);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setStartOffset(0);
        chunk.setEndOffset(content.length());
        chunk = chunkRepository.saveAndFlush(chunk);

        if (withEmbedding) {
            // All vectors have 768 dimensions. Unit vectors make expected
            // cosine similarities deterministic.
            float[] vector = new float[768];
            vector[0] = firstVectorValue;

            StringBuilder literal = new StringBuilder("[");
            for (int i = 0; i < vector.length; i++) {
                if (i > 0) literal.append(',');
                literal.append(vector[i]);
            }
            literal.append(']');

            jdbcTemplate.update(
                    """
                    UPDATE document_chunks
                    SET embedding = CAST(? AS vector)
                    WHERE id = ?
                    """,
                    literal.toString(),
                    chunk.getId());
        }

        return chunk;
    }

    @Test
    void searchReturnsOnlyChunksOwnedByTheSearchingUser() {
        createChunk(userA, "a-document.pdf", 0,
                "User A identity address", 1.0f, true);
        createChunk(userB, "b-document.pdf", 0,
                "User B identity address", 1.0f, true);

        var results = searchService.search(
                userA.getUsername(), "identity address", 10);

        assertEquals(1, results.size());
        assertEquals("a-document.pdf", results.get(0).filename());
    }

    
     @Test
     void searchRanksMoreSimilarVectorsFirst() {
        // Query embedding is [1, 0, 0, ...].
        // This vector is orthogonal to the query.
        float[] lessSimilarVector = new float[768];
        lessSimilarVector[1] = 1.0f;

        createChunk(
                userA,
                "less-similar.pdf",
                0,
                "Less similar content",
                1.0f,
                true
        );

        createChunk(
                userA,
                "most-similar.pdf",
                1,
                "Most similar content",
                1.0f,
                true
        );

        // Replace the first chunk's embedding with a valid,
        // non-zero vector orthogonal to the query vector.
        StringBuilder literal = new StringBuilder("[");
        for (int i = 0; i < lessSimilarVector.length; i++) {
                if (i > 0) {
                literal.append(',');
                }
                literal.append(lessSimilarVector[i]);
        }
        literal.append(']');

        jdbcTemplate.update(
                """
                UPDATE document_chunks
                SET embedding = CAST(? AS vector)
                WHERE id = (
                        SELECT dc.id
                        FROM document_chunks dc
                        JOIN extracted_documents ed
                        ON ed.id = dc.extracted_document_id
                        JOIN kyc_documents kd
                        ON kd.id = ed.document_id
                        WHERE kd.user_id = ?
                        AND kd.original_filename = ?
                )
                """,
                literal.toString(),
                userA.getId(),
                "less-similar.pdf"
        );

        var results = searchService.search(
                userA.getUsername(),
                "identity",
                10
        );

        assertEquals(2, results.size());
        assertEquals("most-similar.pdf", results.get(0).filename());
        assertEquals("less-similar.pdf", results.get(1).filename());
        assertTrue(
                results.get(0).similarity() > results.get(1).similarity(),
                "Expected the more similar chunk to have a higher similarity score"
        );
    }


    @Test
    void searchExcludesChunksWithoutEmbeddings() {
        createChunk(userA, "embedded.pdf", 0,
                "Has embedding", 1.0f, true);
        createChunk(userA, "not-embedded.pdf", 1,
                "Missing embedding", 1.0f, false);

        var results = searchService.search(
                userA.getUsername(), "identity", 10);

        assertEquals(1, results.size());
        assertEquals("embedded.pdf", results.get(0).filename());
    }

    @Test
    void searchReturnsEmptyListWhenUserHasNoEmbeddedChunks() {
        createChunk(userA, "not-embedded.pdf", 0,
                "No embedding available", 1.0f, false);

        var results = searchService.search(
                userA.getUsername(), "identity", 10);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
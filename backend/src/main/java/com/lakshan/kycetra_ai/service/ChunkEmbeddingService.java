
package com.lakshan.kycetra_ai.service;

import com.lakshan.kycetra_ai.entity.DocumentChunk;
import com.lakshan.kycetra_ai.repository.DocumentChunkRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChunkEmbeddingService {

    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingGenerationService embeddingGenerationService;

    public ChunkEmbeddingService(
            DocumentChunkRepository documentChunkRepository,
            EmbeddingGenerationService embeddingGenerationService) {
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingGenerationService = embeddingGenerationService;
    }

    @Transactional
    public int generateAndStoreEmbeddings(Long extractedDocumentId) {

        List<DocumentChunk> chunks =
            documentChunkRepository
                .findAllByExtractedDocument_IdOrderByChunkIndexAsc(
                    extractedDocumentId
                );

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException(
                "No chunks found for the extracted document."
            );
        }

        int updatedCount = 0;

        for (DocumentChunk chunk : chunks) {
            float[] vector =
                embeddingGenerationService.generateEmbedding(
                    chunk.getContent()
                );

            if (vector.length != 768) {
                throw new IllegalStateException(
                    "Expected 768 embedding dimensions but received "
                        + vector.length
                );
            }

            String vectorLiteral = toVectorLiteral(vector);

            int updated = documentChunkRepository.updateEmbedding(
                chunk.getId(),
                vectorLiteral
            );

            if (updated != 1) {
                throw new IllegalStateException(
                    "Failed to store embedding for chunk " + chunk.getId()
                );
            }

            updatedCount++;
        }

        return updatedCount;
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                result.append(',');
            }

            if (!Float.isFinite(vector[i])) {
                throw new IllegalArgumentException(
                    "Embedding contains a non-finite value."
                );
            }

            result.append(vector[i]);
        }

        return result.append(']').toString();
    }
}

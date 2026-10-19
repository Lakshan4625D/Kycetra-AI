
package com.lakshan.kycetra_ai.controller;

import com.lakshan.kycetra_ai.service.ChunkEmbeddingService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/documents")
public class ChunkEmbeddingController {

    private final ChunkEmbeddingService chunkEmbeddingService;

    public ChunkEmbeddingController(
            ChunkEmbeddingService chunkEmbeddingService) {
        this.chunkEmbeddingService = chunkEmbeddingService;
    }

    @PostMapping("/{extractedDocumentId}/generate-embeddings")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> generateEmbeddings(
            @PathVariable Long extractedDocumentId) {

        int count = chunkEmbeddingService
            .generateAndStoreEmbeddings(extractedDocumentId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "extractedDocumentId", extractedDocumentId,
            "embeddingsStored", count
        ));
    }
}

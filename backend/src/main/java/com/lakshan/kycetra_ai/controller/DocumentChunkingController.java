package com.lakshan.kycetra_ai.controller;

import com.lakshan.kycetra_ai.entity.DocumentChunk;
import com.lakshan.kycetra_ai.service.DocumentChunkingService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test/documents")
public class DocumentChunkingController {

    private final DocumentChunkingService documentChunkingService;

    public DocumentChunkingController(
            DocumentChunkingService documentChunkingService) {
        this.documentChunkingService = documentChunkingService;
    }

    @PostMapping("/{extractedDocumentId}/chunks")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> createChunks(
            @PathVariable Long extractedDocumentId) {

        List<DocumentChunk> chunks =
                documentChunkingService.chunkDocument(extractedDocumentId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "extractedDocumentId", extractedDocumentId,
                "chunksCreated", chunks.size()
        ));
    }
}
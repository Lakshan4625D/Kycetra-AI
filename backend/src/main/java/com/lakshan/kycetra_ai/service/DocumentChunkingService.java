package com.lakshan.kycetra_ai.service;

import com.lakshan.kycetra_ai.entity.DocumentChunk;
import com.lakshan.kycetra_ai.entity.ExtractedDocument;
import com.lakshan.kycetra_ai.repository.DocumentChunkRepository;
import com.lakshan.kycetra_ai.repository.ExtractedDocumentRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkingService {

    private final ExtractedDocumentRepository extractedDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    private final int chunkSize;
    private final int chunkOverlap;

    public DocumentChunkingService(
            ExtractedDocumentRepository extractedDocumentRepository,
            DocumentChunkRepository documentChunkRepository,
            @Value("${kycetra.chunking.chunk-size:1000}") int chunkSize,
            @Value("${kycetra.chunking.chunk-overlap:150}") int chunkOverlap) {

        if (chunkSize <= 0) {
            throw new IllegalArgumentException(
                "Chunk size must be greater than zero."
            );
        }

        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException(
                "Chunk overlap must be non-negative and smaller than chunk size."
            );
        }

        this.extractedDocumentRepository = extractedDocumentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    @Transactional
    public List<DocumentChunk> chunkDocument(Long extractedDocumentId) {

        ExtractedDocument extractedDocument =
            extractedDocumentRepository.findById(extractedDocumentId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Extracted document not found."
                ));

        String text = extractedDocument.getExtractedText();

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                "Extracted document contains no text."
            );
        }

        List<DocumentChunk> chunks = new ArrayList<>();

        int start = 0;
        int index = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            DocumentChunk chunk = new DocumentChunk();
            chunk.setExtractedDocument(extractedDocument);
            chunk.setChunkIndex(index++);
            chunk.setContent(text.substring(start, end));
            chunk.setStartOffset(start);
            chunk.setEndOffset(end);

            chunks.add(chunk);

            if (end == text.length()) {
                break;
            }

            start = end - chunkOverlap;
        }

        // Replace old chunks so reprocessing cannot create duplicates.
        documentChunkRepository
            .deleteAllByExtractedDocument_Id(extractedDocumentId);

        return documentChunkRepository.saveAll(chunks);
    }
}
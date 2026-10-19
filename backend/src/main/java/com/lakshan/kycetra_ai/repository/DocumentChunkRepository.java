package com.lakshan.kycetra_ai.repository;

import com.lakshan.kycetra_ai.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk>
        findAllByExtractedDocument_IdOrderByChunkIndexAsc(
            Long extractedDocumentId
        );

    void deleteAllByExtractedDocument_Id(Long extractedDocumentId);
}
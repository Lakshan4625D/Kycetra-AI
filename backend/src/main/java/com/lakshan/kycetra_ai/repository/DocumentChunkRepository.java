
package com.lakshan.kycetra_ai.repository;

import com.lakshan.kycetra_ai.entity.DocumentChunk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk>
        findAllByExtractedDocument_IdOrderByChunkIndexAsc(
            Long extractedDocumentId
        );

    void deleteAllByExtractedDocument_Id(Long extractedDocumentId);

    @Transactional
    @Modifying
    @Query(value = """
        UPDATE document_chunks
        SET embedding = CAST(:embedding AS vector)
        WHERE id = :chunkId
        """, nativeQuery = true)
    int updateEmbedding(
        @Param("chunkId") Long chunkId,
        @Param("embedding") String embedding
    );
}

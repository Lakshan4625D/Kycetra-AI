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

    @Query(value = """
        SELECT
            dc.id AS "chunkId",
            kd.id AS "documentId",
            kd.original_filename AS "filename",
            dc.chunk_index AS "chunkIndex",
            dc.content AS "content",
            (1.0 - (dc.embedding <=> CAST(:embedding AS vector)))
                AS "similarity"
        FROM document_chunks dc
        JOIN extracted_documents ed
            ON ed.id = dc.extracted_document_id
        JOIN kyc_documents kd
            ON kd.id = ed.document_id
        WHERE kd.user_id = :userId
          AND dc.embedding IS NOT NULL
        ORDER BY dc.embedding <=> CAST(:embedding AS vector) ASC
        LIMIT :resultLimit
        """, nativeQuery = true)
    List<SemanticSearchProjection> searchSimilarChunks(
        @Param("userId") Long userId,
        @Param("embedding") String embedding,
        @Param("resultLimit") int resultLimit
    );
}

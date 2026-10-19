package com.lakshan.kycetra_ai.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "document_chunks",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_document_chunks_index",
            columnNames = {"extracted_document_id", "chunk_index"}
        )
    },
    indexes = {
        @Index(
            name = "idx_document_chunks_extracted_document",
            columnList = "extracted_document_id"
        )
    }
)
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "extracted_document_id", nullable = false)
    private ExtractedDocument extractedDocument;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "start_offset", nullable = false)
    private Integer startOffset;

    @Column(name = "end_offset", nullable = false)
    private Integer endOffset;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public DocumentChunk() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ExtractedDocument getExtractedDocument() {
        return extractedDocument;
    }

    public void setExtractedDocument(ExtractedDocument extractedDocument) {
        this.extractedDocument = extractedDocument;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(Integer startOffset) {
        this.startOffset = startOffset;
    }

    public Integer getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(Integer endOffset) {
        this.endOffset = endOffset;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
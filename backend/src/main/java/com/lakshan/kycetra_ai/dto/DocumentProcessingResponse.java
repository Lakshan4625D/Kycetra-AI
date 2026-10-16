package com.lakshan.kycetra_ai.dto;

import com.lakshan.kycetra_ai.entity.DocumentStatus;

import java.time.OffsetDateTime;

public record DocumentProcessingResponse(
        Long documentId,
        DocumentStatus status,
        Integer characterCount,
        OffsetDateTime processedAt
) {
}
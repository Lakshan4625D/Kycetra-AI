
package com.lakshan.kycetra_ai.dto;

import com.lakshan.kycetra_ai.entity.DocumentStatus;
import java.time.OffsetDateTime;

public record DocumentUploadResponse(
        Long id,
        String filename,
        String contentType,
        Long fileSizeBytes,
        DocumentStatus status,
        OffsetDateTime createdAt
) {
}

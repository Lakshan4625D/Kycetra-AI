package com.lakshan.kycetra_ai.dto;

import java.util.List;

public record KycQuestionResponse(
        String answer,
        List<Source> sources
) {
    public record Source(
            int sourceNumber,
            Long documentId,
            String filename,
            Integer chunkIndex,
            String excerpt,
            Double similarity
    ) {
    }
}
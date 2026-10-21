package com.lakshan.kycetra_ai.dto;

public record SemanticSearchResult(
        Long chunkId,
        Long documentId,
        String filename,
        Integer chunkIndex,
        String content,
        Double similarity
) {
}

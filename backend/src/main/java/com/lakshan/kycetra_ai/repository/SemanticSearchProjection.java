package com.lakshan.kycetra_ai.repository;

public interface SemanticSearchProjection {

    Long getChunkId();

    Long getDocumentId();

    String getFilename();

    Integer getChunkIndex();

    String getContent();

    Double getSimilarity();
}

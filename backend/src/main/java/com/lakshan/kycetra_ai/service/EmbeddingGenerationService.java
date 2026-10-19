
package com.lakshan.kycetra_ai.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingGenerationService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingGenerationService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                "Text must not be empty."
            );
        }

        return embeddingModel.embed(text);
    }
}

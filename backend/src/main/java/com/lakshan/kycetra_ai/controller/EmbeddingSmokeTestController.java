
package com.lakshan.kycetra_ai.controller;

import com.lakshan.kycetra_ai.service.EmbeddingGenerationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class EmbeddingSmokeTestController {

    private final EmbeddingGenerationService embeddingService;

    public EmbeddingSmokeTestController(
            EmbeddingGenerationService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping("/embedding-smoke-test")
    public Map<String, Object> testEmbedding() {
        float[] embedding = embeddingService.generateEmbedding(
            "A customer submitted an identity document for KYC verification."
        );

        return Map.of(
            "success", true,
            "dimensions", embedding.length,
            "preview", java.util.Arrays.copyOf(embedding,
                Math.min(5, embedding.length))
        );
    }
}

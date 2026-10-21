package com.lakshan.kycetra_ai.service;

import com.lakshan.kycetra_ai.dto.SemanticSearchResult;
import com.lakshan.kycetra_ai.entity.User;
import com.lakshan.kycetra_ai.repository.DocumentChunkRepository;
import com.lakshan.kycetra_ai.repository.SemanticSearchProjection;
import com.lakshan.kycetra_ai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class SemanticSearchService {

    private static final int EMBEDDING_DIMENSIONS = 768;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final DocumentChunkRepository chunkRepository;
    private final UserRepository userRepository;
    private final EmbeddingGenerationService embeddingService;

    public SemanticSearchService(
            DocumentChunkRepository chunkRepository,
            UserRepository userRepository,
            EmbeddingGenerationService embeddingService) {
        this.chunkRepository = chunkRepository;
        this.userRepository = userRepository;
        this.embeddingService = embeddingService;
    }

    @Transactional(readOnly = true)
    public List<SemanticSearchResult> search(
            String username,
            String query,
            int limit) {

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Search query must not be empty.");
        }

        if (query.length() > 2000) {
            throw new IllegalArgumentException(
                    "Search query must not exceed 2000 characters.");
        }

        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and " + MAX_LIMIT + ".");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Authenticated user was not found."));

        float[] vector = embeddingService.generateEmbedding(
                query.trim());

        if (vector.length != EMBEDDING_DIMENSIONS) {
            throw new IllegalStateException(
                    "Expected a " + EMBEDDING_DIMENSIONS
                            + "-dimension embedding, but received "
                            + vector.length + ".");
        }

        String embeddingLiteral = toVectorLiteral(vector);

        List<SemanticSearchProjection> matches =
                chunkRepository.searchSimilarChunks(
                        user.getId(), embeddingLiteral, limit);

        return matches.stream()
                .map(match -> new SemanticSearchResult(
                        match.getChunkId(),
                        match.getDocumentId(),
                        match.getFilename(),
                        match.getChunkIndex(),
                        match.getContent(),
                        match.getSimilarity()))
                .toList();
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                result.append(',');
            }
            result.append(
                    String.format(Locale.ROOT, "%s", vector[i]));
        }
        return result.append(']').toString();
    }
}

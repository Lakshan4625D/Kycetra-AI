package com.lakshan.kycetra_ai.service;

import com.lakshan.kycetra_ai.dto.SemanticSearchResult;
import com.lakshan.kycetra_ai.entity.User;
import com.lakshan.kycetra_ai.repository.DocumentChunkRepository;
import com.lakshan.kycetra_ai.repository.SemanticSearchProjection;
import com.lakshan.kycetra_ai.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    @Mock
    private DocumentChunkRepository chunkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmbeddingGenerationService embeddingService;

    @InjectMocks
    private SemanticSearchService searchService;

    @Test
    void searchReturnsMappedResultsForAuthenticatedUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(user));

        when(embeddingService.generateEmbedding("address verification"))
                .thenReturn(new float[768]);

        SemanticSearchProjection match = mock(SemanticSearchProjection.class);
        when(match.getChunkId()).thenReturn(2L);
        when(match.getDocumentId()).thenReturn(68L);
        when(match.getFilename()).thenReturn("fictional_kyc_test_document.pdf");
        when(match.getChunkIndex()).thenReturn(1);
        when(match.getContent()).thenReturn("Customer address verification details");
        when(match.getSimilarity()).thenReturn(0.87);

        when(chunkRepository.searchSimilarChunks(
                eq(42L), anyString(), eq(5)))
                .thenReturn(List.of(match));

        List<SemanticSearchResult> results =
                searchService.search("alice", "address verification", 5);

        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).chunkId());
        assertEquals(68L, results.get(0).documentId());
        assertEquals("fictional_kyc_test_document.pdf",
                results.get(0).filename());
        assertEquals(1, results.get(0).chunkIndex());
        assertEquals("Customer address verification details",
                results.get(0).content());
        assertEquals(0.87, results.get(0).similarity());

        verify(embeddingService).generateEmbedding("address verification");
        verify(chunkRepository).searchSimilarChunks(
                eq(42L), anyString(), eq(5));
    }

    @Test
    void searchReturnsEmptyListWhenNoChunksMatch() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(user));
        when(embeddingService.generateEmbedding("unknown topic"))
                .thenReturn(new float[768]);

        when(chunkRepository.searchSimilarChunks(
                eq(42L), anyString(), eq(5)))
                .thenReturn(List.of());

        List<SemanticSearchResult> results =
                searchService.search("alice", "unknown topic", 5);

        assertTrue(results.isEmpty());
    }

    @Test
    void searchRejectsEmptyQuery() {
        assertThrows(IllegalArgumentException.class,
                () -> searchService.search("alice", "  ", 5));

        verifyNoInteractions(userRepository, embeddingService, chunkRepository);
    }

    @Test
    void searchRejectsQueryLongerThan2000Characters() {
        String query = "a".repeat(2001);

        assertThrows(IllegalArgumentException.class,
                () -> searchService.search("alice", query, 5));

        verifyNoInteractions(userRepository, embeddingService, chunkRepository);
    }

    @Test
    void searchRejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> searchService.search("alice", "address", 0));

        assertThrows(IllegalArgumentException.class,
                () -> searchService.search("alice", "address", 21));

        verifyNoInteractions(userRepository, embeddingService, chunkRepository);
    }

    @Test
    void searchRejectsEmbeddingWithIncorrectDimensions() {
        User user = mock(User.class);
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(user));
        when(embeddingService.generateEmbedding("address"))
                .thenReturn(new float[100]);

        assertThrows(IllegalStateException.class,
                () -> searchService.search("alice", "address", 5));

        verifyNoInteractions(chunkRepository);
    }

    @Test
    void searchUsesAuthenticatedUsersIdForRepositoryFiltering() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(99L);
        when(userRepository.findByUsername("bob"))
                .thenReturn(Optional.of(user));
        when(embeddingService.generateEmbedding("identity"))
                .thenReturn(new float[768]);
        when(chunkRepository.searchSimilarChunks(
                eq(99L), anyString(), eq(3)))
                .thenReturn(List.of());

        searchService.search("bob", "identity", 3);

        verify(chunkRepository).searchSimilarChunks(
                eq(99L), anyString(), eq(3));
        verify(userRepository).findByUsername("bob");
    }
}
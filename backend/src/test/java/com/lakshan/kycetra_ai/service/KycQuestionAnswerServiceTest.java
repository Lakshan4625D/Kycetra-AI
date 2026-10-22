
package com.lakshan.kycetra_ai.service;

import com.lakshan.kycetra_ai.dto.KycQuestionResponse;
import com.lakshan.kycetra_ai.dto.SemanticSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycQuestionAnswerServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Mock
    private SemanticSearchService semanticSearchService;

    private KycQuestionAnswerService questionAnswerService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);

        questionAnswerService = new KycQuestionAnswerService(
                chatClientBuilder,
                semanticSearchService);
    }

    private void mockGeminiResponse(String answer) {
        when(chatClient.prompt()).thenReturn(requestSpec);

        doReturn(requestSpec)
                .when(requestSpec)
                .system(anyString());

        doReturn(requestSpec)
                .when(requestSpec)
                .user(anyString());

        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(answer);
    }

    private SemanticSearchResult sampleResult() {
        return new SemanticSearchResult(
                10L,
                68L,
                "fictional_kyc_test_document.pdf",
                0,
                "Full name: Mira Sample. Date of birth: 15 January 1990.",
                0.82);
    }

    @Test
    void answerReturnsGeminiAnswerAndMapsSources() {
        when(semanticSearchService.search(
                "alice", "What is the applicant's name?", 5))
                .thenReturn(List.of(sampleResult()));

        mockGeminiResponse("The applicant is Mira Sample [Source 1].");

        KycQuestionResponse result = questionAnswerService.answer(
                "alice", "  What is the applicant's name?  ");

        assertEquals(
                "The applicant is Mira Sample [Source 1].",
                result.answer());

        assertEquals(1, result.sources().size());

        KycQuestionResponse.Source source = result.sources().get(0);
        assertEquals(1, source.sourceNumber());
        assertEquals(68L, source.documentId());
        assertEquals("fictional_kyc_test_document.pdf", source.filename());
        assertEquals(0, source.chunkIndex());
        assertEquals(
                "Full name: Mira Sample. Date of birth: 15 January 1990.",
                source.excerpt());
        assertEquals(0.82, source.similarity());

        verify(semanticSearchService).search(
                "alice", "What is the applicant's name?", 5);
        verify(chatClient).prompt();
    }

    @Test
    void answerReturnsFallbackWhenNoEvidenceIsFound() {
        when(semanticSearchService.search("alice", "unknown topic", 5))
                .thenReturn(List.of());

        KycQuestionResponse result = questionAnswerService.answer(
                "alice", "unknown topic");

        assertTrue(result.answer().contains(
                "couldn't find relevant information"));
        assertTrue(result.sources().isEmpty());

        verifyNoInteractions(chatClient);
    }

    @Test
    void answerRejectsBlankQuestion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> questionAnswerService.answer("alice", "  "));

        verifyNoInteractions(semanticSearchService, chatClient);
    }

    @Test
    void answerRejectsNullQuestion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> questionAnswerService.answer("alice", null));

        verifyNoInteractions(semanticSearchService, chatClient);
    }

    @Test
    void answerRejectsQuestionLongerThan2000Characters() {
        String question = "a".repeat(2001);

        assertThrows(
                IllegalArgumentException.class,
                () -> questionAnswerService.answer("alice", question));

        verifyNoInteractions(semanticSearchService, chatClient);
    }

    @Test
    void answerReturnsFallbackWhenGeminiReturnsNull() {
        when(semanticSearchService.search("alice", "identity", 5))
                .thenReturn(List.of(sampleResult()));

        mockGeminiResponse(null);

        KycQuestionResponse result = questionAnswerService.answer(
                "alice", "identity");

        assertTrue(result.answer().contains(
                "couldn't produce an answer"));
        assertEquals(1, result.sources().size());
    }

    @Test
    void answerReturnsFallbackWhenGeminiReturnsBlankContent() {
        when(semanticSearchService.search("alice", "identity", 5))
                .thenReturn(List.of(sampleResult()));

        mockGeminiResponse("   ");

        KycQuestionResponse result = questionAnswerService.answer(
                "alice", "identity");

        assertTrue(result.answer().contains(
                "couldn't produce an answer"));
        assertEquals(1, result.sources().size());
    }

    @Test
    void answerPassesRetrievedEvidenceToGemini() {
        when(semanticSearchService.search("alice", "identity", 5))
                .thenReturn(List.of(sampleResult()));

        mockGeminiResponse("The applicant is Mira Sample [Source 1].");

        questionAnswerService.answer("alice", "identity");

        verify(requestSpec).system(argThat(
                (String prompt) ->
                        prompt.contains("supplied document evidence")
                                && prompt.contains("untrusted data")));

        verify(requestSpec).user(argThat(
                (String prompt) ->
                        prompt.contains("identity")
                                && prompt.contains("[Source 1]")
                                && prompt.contains("Mira Sample")
                                && prompt.contains(
                                        "fictional_kyc_test_document.pdf")));
    }

    @Test
    void answerPropagatesRetrievalFailure() {
        when(semanticSearchService.search("alice", "identity", 5))
                .thenThrow(new IllegalStateException("Retrieval failed"));

        assertThrows(
                IllegalStateException.class,
                () -> questionAnswerService.answer("alice", "identity"));

        verifyNoInteractions(chatClient);
    }
}

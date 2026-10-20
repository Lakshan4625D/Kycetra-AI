
package com.lakshan.kycetra_ai.service;

import com.lakshan.kycetra_ai.entity.DocumentStatus;
import com.lakshan.kycetra_ai.entity.ExtractedDocument;
import com.lakshan.kycetra_ai.entity.KycDocument;
import com.lakshan.kycetra_ai.entity.User;
import com.lakshan.kycetra_ai.repository.ExtractedDocumentRepository;
import com.lakshan.kycetra_ai.repository.KycDocumentRepository;
import com.lakshan.kycetra_ai.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class KycDocumentProcessingService {

    private final KycDocumentRepository documentRepository;
    private final ExtractedDocumentRepository extractedDocumentRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService storageService;
    private final PdfTextExtractionService extractionService;
    private final DocumentChunkingService chunkingService;
    private final ChunkEmbeddingService embeddingService;

    public KycDocumentProcessingService(
            KycDocumentRepository documentRepository,
            ExtractedDocumentRepository extractedDocumentRepository,
            UserRepository userRepository,
            DocumentStorageService storageService,
            PdfTextExtractionService extractionService,
            DocumentChunkingService chunkingService,
            ChunkEmbeddingService embeddingService) {

        this.documentRepository = documentRepository;
        this.extractedDocumentRepository = extractedDocumentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.extractionService = extractionService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
    }

    public ExtractedDocument processDocument(
            String username,
            Long documentId) throws IOException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        KycDocument document = documentRepository
                .findByIdAndUser_Id(documentId, user.getId())
                .orElseThrow(() ->
                        new java.util.NoSuchElementException(
                                "Document not found."));

        if (!"application/pdf".equals(document.getContentType())) {
            throw new IllegalArgumentException(
                    "Only PDF documents can be processed currently.");
        }

        document.setStatus(DocumentStatus.PROCESSING);
        document.setFailureReason(null);
        documentRepository.saveAndFlush(document);

        try {
            // Stage 1: Extract text from the stored PDF.
            Path pdfPath = storageService.getStoredFilePath(
                    document.getStorageKey());

            String extractedText = extractionService.extractText(pdfPath);

            // Stage 2: Save or update the extracted text.
            ExtractedDocument extractedDocument =
                    extractedDocumentRepository
                            .findByDocument_Id(document.getId())
                            .orElseGet(ExtractedDocument::new);

            extractedDocument.setDocument(document);
            extractedDocument.setExtractedText(extractedText);

            ExtractedDocument saved =
                    extractedDocumentRepository.save(extractedDocument);

            // Stage 3: Create fresh chunks.
            chunkingService.chunkDocument(saved.getId());

            // Stage 4: Generate and store an embedding for every chunk.
            embeddingService.generateAndStoreEmbeddings(saved.getId());

            // Only mark completed after all stages succeed.
            document.setStatus(DocumentStatus.COMPLETED);
            document.setFailureReason(null);
            documentRepository.save(document);

            return saved;

        } catch (IOException | RuntimeException exception) {
            document.setStatus(DocumentStatus.FAILED);
            document.setFailureReason(
                    "Document processing failed: "
                            + safeFailureMessage(exception));

            documentRepository.save(document);

            throw exception;
        }
    }

    private String safeFailureMessage(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        // Avoid storing excessively long exception messages.
        return message.length() > 1000
                ? message.substring(0, 1000)
                : message;
    }
}

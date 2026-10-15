
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

    public KycDocumentProcessingService(
            KycDocumentRepository documentRepository,
            ExtractedDocumentRepository extractedDocumentRepository,
            UserRepository userRepository,
            DocumentStorageService storageService,
            PdfTextExtractionService extractionService
    ) {
        this.documentRepository = documentRepository;
        this.extractedDocumentRepository = extractedDocumentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.extractionService = extractionService;
    }

    public ExtractedDocument processDocument(
            String username,
            Long documentId
    ) throws IOException {

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
        documentRepository.saveAndFlush(document);

        try {
            Path pdfPath = storageService.getStoredFilePath(
                    document.getStorageKey()
            );

            String extractedText = extractionService.extractText(pdfPath);

            ExtractedDocument extractedDocument =
                    extractedDocumentRepository
                            .findByDocument_Id(document.getId())
                            .orElseGet(ExtractedDocument::new);

            extractedDocument.setDocument(document);
            extractedDocument.setExtractedText(extractedText);

            ExtractedDocument saved =
                    extractedDocumentRepository.save(extractedDocument);

            document.setStatus(DocumentStatus.COMPLETED);
            document.setFailureReason(null);
            documentRepository.save(document);

            return saved;

        } catch (IOException | RuntimeException exception) {
            document.setStatus(DocumentStatus.FAILED);
            document.setFailureReason(
                    "Document processing failed: "
                            + exception.getMessage()
            );
            documentRepository.save(document);

            throw exception;
        }
    }
}

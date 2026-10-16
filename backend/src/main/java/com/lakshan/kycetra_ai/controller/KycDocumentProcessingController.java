package com.lakshan.kycetra_ai.controller;

import com.lakshan.kycetra_ai.dto.DocumentProcessingResponse;
import com.lakshan.kycetra_ai.entity.ExtractedDocument;
import com.lakshan.kycetra_ai.service.KycDocumentProcessingService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/documents")
public class KycDocumentProcessingController {

    private final KycDocumentProcessingService processingService;

    public KycDocumentProcessingController(
            KycDocumentProcessingService processingService) {
        this.processingService = processingService;
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<DocumentProcessingResponse> processDocument(
            @PathVariable Long id,
            Authentication authentication
    ) throws IOException {

        ExtractedDocument extractedDocument =
                processingService.processDocument(
                        authentication.getName(),
                        id
                );

        DocumentProcessingResponse response =
                new DocumentProcessingResponse(
                        extractedDocument.getDocument().getId(),
                        extractedDocument.getDocument().getStatus(),
                        extractedDocument.getCharacterCount(),
                        extractedDocument.getUpdatedAt()
                );

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(
            NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Document not found.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidRequest(
            IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(exception.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleProcessingFailure(
            IOException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Document processing failed.");
    }
}
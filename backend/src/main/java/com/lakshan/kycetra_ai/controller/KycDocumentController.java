
package com.lakshan.kycetra_ai.controller;

import com.lakshan.kycetra_ai.dto.DocumentUploadResponse;
import com.lakshan.kycetra_ai.service.KycDocumentProcessingService;
import com.lakshan.kycetra_ai.service.KycDocumentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class KycDocumentController {

    private final KycDocumentService documentService;
    private final KycDocumentProcessingService processingService;

    public KycDocumentController(
            KycDocumentService documentService,
            KycDocumentProcessingService processingService) {
        this.documentService = documentService;
        this.processingService = processingService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        // Upload and persist the file first.
        DocumentUploadResponse uploaded;

        try {
            uploaded = documentService.upload(
                    authentication.getName(), file);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage()));
        }

        // Images remain UPLOADED until OCR is implemented.
        if (!"application/pdf".equals(uploaded.contentType())) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(uploaded);
        }

        // PDFs are processed automatically.
        try {
            processingService.processDocument(
                    authentication.getName(), uploaded.id());

        } catch (IOException | RuntimeException exception) {
            // The file was already uploaded. Return its actual status
            // rather than implying that the upload itself was lost.
            DocumentUploadResponse current =
                    documentService.getMyDocument(
                            authentication.getName(), uploaded.id());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(current);
        }

        // Fetch the updated status after processing.
        DocumentUploadResponse processed =
                documentService.getMyDocument(
                        authentication.getName(), uploaded.id());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(processed);
    }

    @GetMapping
    public ResponseEntity<?> getMyDocuments(
            Authentication authentication) {
        try {
            return ResponseEntity.ok(
                    documentService.getMyDocuments(
                            authentication.getName()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMyDocument(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(
                    documentService.getMyDocument(
                            authentication.getName(), id));
        } catch (java.util.NoSuchElementException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Document not found."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage()));
        }
    }
}

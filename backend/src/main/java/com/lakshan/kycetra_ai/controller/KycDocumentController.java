
package com.lakshan.kycetra_ai.controller;

import com.lakshan.kycetra_ai.dto.DocumentUploadResponse;
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

    public KycDocumentController(KycDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws IOException {
        try {
            DocumentUploadResponse response = documentService.upload(
                    authentication.getName(),
                    file
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage())
            );
        }
    }
    
@GetMapping
public ResponseEntity<?> getMyDocuments(Authentication authentication) {
    try {
        return ResponseEntity.ok(
                documentService.getMyDocuments(authentication.getName())
        );
    } catch (IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(
                Map.of("error", exception.getMessage())
        );
    }
}

    @GetMapping("/{id}")
    public ResponseEntity<?> getMyDocument(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            return ResponseEntity.ok(
                    documentService.getMyDocument(authentication.getName(), id)
            );
        } catch (java.util.NoSuchElementException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Document not found.")
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage())
            );
        }
    }
}

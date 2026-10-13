
package com.lakshan.kycetra_ai.service;

import com.lakshan.kycetra_ai.dto.DocumentUploadResponse;
import com.lakshan.kycetra_ai.entity.KycDocument;
import com.lakshan.kycetra_ai.entity.User;
import com.lakshan.kycetra_ai.repository.KycDocumentRepository;
import com.lakshan.kycetra_ai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class KycDocumentService {

    private final UserRepository userRepository;
    private final KycDocumentRepository documentRepository;
    private final DocumentStorageService storageService;

    public KycDocumentService(
            UserRepository userRepository,
            KycDocumentRepository documentRepository,
            DocumentStorageService storageService
    ) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.storageService = storageService;
    }

    @Transactional
    public DocumentUploadResponse upload(
            String username,
            MultipartFile file
    ) throws IOException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Authenticated user not found."));

        DocumentStorageService.StoredFile storedFile =
                storageService.store(file);

        try {
            KycDocument document = new KycDocument();
            document.setUser(user);
            document.setOriginalFilename(storedFile.originalFilename());
            document.setStorageKey(storedFile.storageKey());
            document.setContentType(storedFile.contentType());
            document.setFileSizeBytes(storedFile.fileSizeBytes());

            KycDocument saved = documentRepository.save(document);

            return new DocumentUploadResponse(
                    saved.getId(),
                    saved.getOriginalFilename(),
                    saved.getContentType(),
                    saved.getFileSizeBytes(),
                    saved.getStatus(),
                    saved.getCreatedAt()
            );
        } catch (RuntimeException exception) {
            try {
                storageService.delete(storedFile.storageKey());
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }
    }
 
    @Transactional(readOnly = true)
    public java.util.List<DocumentUploadResponse> getMyDocuments(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Authenticated user not found."));

        return documentRepository
                .findAllByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(document -> new DocumentUploadResponse(
                        document.getId(),
                        document.getOriginalFilename(),
                        document.getContentType(),
                        document.getFileSizeBytes(),
                        document.getStatus(),
                        document.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentUploadResponse getMyDocument(String username, Long documentId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Authenticated user not found."));

        KycDocument document = documentRepository
                .findByIdAndUser_Id(documentId, user.getId())
                .orElseThrow(() ->
                        new java.util.NoSuchElementException("Document not found."));

        return new DocumentUploadResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getFileSizeBytes(),
                document.getStatus(),
                document.getCreatedAt()
        );
    }
}

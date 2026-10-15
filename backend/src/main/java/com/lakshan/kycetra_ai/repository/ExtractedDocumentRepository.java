
package com.lakshan.kycetra_ai.repository;

import com.lakshan.kycetra_ai.entity.ExtractedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExtractedDocumentRepository
        extends JpaRepository<ExtractedDocument, Long> {

    Optional<ExtractedDocument> findByDocument_Id(Long documentId);
}

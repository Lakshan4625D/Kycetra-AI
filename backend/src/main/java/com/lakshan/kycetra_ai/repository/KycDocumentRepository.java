
package com.lakshan.kycetra_ai.repository;

import com.lakshan.kycetra_ai.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository
        extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<KycDocument> findByIdAndUser_Id(Long id, Long userId);
}

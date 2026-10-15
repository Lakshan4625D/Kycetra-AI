CREATE TABLE extracted_documents (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL UNIQUE,
    extracted_text TEXT NOT NULL,
    character_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_extracted_documents_document
        FOREIGN KEY (document_id)
        REFERENCES kyc_documents(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_extracted_documents_character_count
        CHECK (character_count >= 0)
);

CREATE INDEX idx_extracted_documents_document_id
    ON extracted_documents(document_id);
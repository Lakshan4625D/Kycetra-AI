
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,

    extracted_document_id BIGINT NOT NULL,

    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,

    start_offset INTEGER NOT NULL,
    end_offset INTEGER NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_chunks_extracted_document
        FOREIGN KEY (extracted_document_id)
        REFERENCES extracted_documents(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_document_chunks_index
        UNIQUE (extracted_document_id, chunk_index),

    CONSTRAINT chk_document_chunks_index
        CHECK (chunk_index >= 0),

    CONSTRAINT chk_document_chunks_offsets
        CHECK (
            start_offset >= 0
            AND end_offset > start_offset
        ),

    CONSTRAINT chk_document_chunks_content
        CHECK (length(content) > 0)
);

CREATE INDEX idx_document_chunks_extracted_document
    ON document_chunks(extracted_document_id);

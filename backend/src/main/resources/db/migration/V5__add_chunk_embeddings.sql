CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE document_chunks
    ADD COLUMN embedding vector(768);

CREATE INDEX idx_document_chunks_embedding
    ON document_chunks
    USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

CREATE TABLE kyc_documents (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    failure_reason TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_kyc_documents_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_kyc_documents_file_size
        CHECK (file_size_bytes > 0),

    CONSTRAINT chk_kyc_documents_status
        CHECK (
            status IN (
                'UPLOADED',
                'PROCESSING',
                'COMPLETED',
                'FAILED'
            )
        )
);

CREATE INDEX idx_kyc_documents_user_id
    ON kyc_documents(user_id);

CREATE INDEX idx_kyc_documents_status
    ON kyc_documents(status);

CREATE INDEX idx_kyc_documents_created_at
    ON kyc_documents(created_at DESC);

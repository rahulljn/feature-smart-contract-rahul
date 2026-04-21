-- V8: Certificates table
-- Tracks PFX/PEM certificates used for PDF digital signing.
-- Actual cert stored in AWS Secrets Manager; this table tracks metadata.

CREATE TABLE certificates (
    cert_id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name       VARCHAR(500) NOT NULL,
    subject         TEXT,
    issuer          TEXT,
    valid_from      TIMESTAMP,
    valid_to        TIMESTAMP,
    thumbprint      VARCHAR(200),
    secret_name     VARCHAR(500) NOT NULL DEFAULT 'my-test-cert',
    s3_key          VARCHAR(1000),
    is_active       BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_by     UUID         REFERENCES users(user_id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Seed current live cert reference
INSERT INTO certificates (file_name, subject, secret_name, is_active)
VALUES ('my-test-cert', 'Geojit Digital Signing Certificate', 'my-test-cert', TRUE);

CREATE INDEX idx_cert_is_active  ON certificates(is_active);
CREATE INDEX idx_cert_valid_to   ON certificates(valid_to);

-- V3: Job Customers table
-- Per-customer pipeline tracking within a job.
-- NOT a client master — ephemeral process tracking only.
-- partyCode + email are copied from pipeline events, not stored as master data.

CREATE TABLE job_customers (
    id                  BIGSERIAL    PRIMARY KEY,
    job_id              UUID         NOT NULL REFERENCES jobs(job_id) ON DELETE CASCADE,
    party_code          VARCHAR(100) NOT NULL,
    contract_note_no    VARCHAR(200),
    trade_date          VARCHAR(50),
    email               VARCHAR(500),
    pdf_status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING'
                            CHECK (pdf_status IN ('PENDING','GENERATED','FAILED')),
    pdf_s3_key          VARCHAR(1000),
    email_status        VARCHAR(50)  NOT NULL DEFAULT 'PENDING'
                            CHECK (email_status IN ('PENDING','SENT','BOUNCED','DELIVERED','FAILED','SKIPPED')),
    ses_message_id      VARCHAR(500),
    bounce_type         VARCHAR(100),
    bounce_reason       TEXT,
    pdf_generated_at    TIMESTAMP,
    email_sent_at       TIMESTAMP,
    delivered_at        TIMESTAMP,
    bounced_at          TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_job_party UNIQUE (job_id, party_code)
);

CREATE INDEX idx_jc_job_id       ON job_customers(job_id);
CREATE INDEX idx_jc_party_code   ON job_customers(party_code);
CREATE INDEX idx_jc_email_status ON job_customers(email_status);
CREATE INDEX idx_jc_pdf_status   ON job_customers(pdf_status);
CREATE INDEX idx_jc_ses_msg_id   ON job_customers(ses_message_id);

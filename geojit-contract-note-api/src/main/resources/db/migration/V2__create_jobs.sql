-- V2: Jobs table
-- Each raw file upload from the ops team is one "job".
-- NO client master data stored here — only pipeline tracking.

CREATE TABLE jobs (
    job_id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name               VARCHAR(500) NOT NULL,
    raw_s3_key              VARCHAR(1000),
    uploaded_by             UUID         REFERENCES users(user_id),
    uploaded_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    status                  VARCHAR(50)  NOT NULL DEFAULT 'VALIDATING'
                                CHECK (status IN (
                                    'VALIDATING','SPLITTING','PROCESSING',
                                    'EMAILING','COMPLETED','FAILED','PARTIAL'
                                )),
    segment_type            VARCHAR(100),
    trade_date              DATE,
    total_customers         INTEGER      NOT NULL DEFAULT 0,
    processed_count         INTEGER      NOT NULL DEFAULT 0,
    pdf_generated_count     INTEGER      NOT NULL DEFAULT 0,
    email_sent_count        INTEGER      NOT NULL DEFAULT 0,
    email_delivered_count   INTEGER      NOT NULL DEFAULT 0,
    email_bounced_count     INTEGER      NOT NULL DEFAULT 0,
    failed_count            INTEGER      NOT NULL DEFAULT 0,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jobs_status       ON jobs(status);
CREATE INDEX idx_jobs_uploaded_at  ON jobs(uploaded_at DESC);
CREATE INDEX idx_jobs_trade_date   ON jobs(trade_date);
CREATE INDEX idx_jobs_uploaded_by  ON jobs(uploaded_by);

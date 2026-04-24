-- V19: Lambda exception tracking from dedicated SQS queue
CREATE TABLE lambda_exceptions (
    id            BIGSERIAL    PRIMARY KEY,
    job_id        VARCHAR(255) NOT NULL,
    lambda_name   VARCHAR(100) NOT NULL,
    record_id     VARCHAR(255),
    error_type    VARCHAR(500),
    error_message TEXT,
    stack_trace   TEXT,
    environment   VARCHAR(50),
    occurred_at   TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_le_job_id      ON lambda_exceptions(job_id);
CREATE INDEX idx_le_lambda_name ON lambda_exceptions(lambda_name);
CREATE INDEX idx_le_occurred_at ON lambda_exceptions(occurred_at DESC);

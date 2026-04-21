-- V4: Pipeline Events table
-- Immutable event log from all Lambdas.
-- Single source of truth for pipeline observability.

CREATE TABLE pipeline_events (
    event_id        BIGSERIAL       PRIMARY KEY,
    job_id          UUID,
    party_code      VARCHAR(100),
    lambda_name     VARCHAR(200)    NOT NULL,
    event_type      VARCHAR(100)    NOT NULL
                        CHECK (event_type IN (
                            'SPLIT_COMPLETE','PDF_TRIGGERED','PDF_GENERATED','PDF_FAILED',
                            'EMAIL_SENT','EMAIL_FAILED','EMAIL_SKIPPED',
                            'DELIVERY','BOUNCE','COMPLAINT'
                        )),
    payload         JSONB,
    event_timestamp TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pe_job_id        ON pipeline_events(job_id);
CREATE INDEX idx_pe_party_code    ON pipeline_events(party_code);
CREATE INDEX idx_pe_event_type    ON pipeline_events(event_type);
CREATE INDEX idx_pe_timestamp     ON pipeline_events(event_timestamp DESC);
CREATE INDEX idx_pe_job_party     ON pipeline_events(job_id, party_code);

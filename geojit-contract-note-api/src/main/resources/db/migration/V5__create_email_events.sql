-- V5: Email Events table
-- SES delivery/bounce/complaint event log from PullBounce and PullDeliver lambdas.

CREATE TABLE email_events (
    id                  BIGSERIAL    PRIMARY KEY,
    ses_message_id      VARCHAR(500),
    party_code          VARCHAR(100),
    job_id              UUID,
    event_type          VARCHAR(100) NOT NULL
                            CHECK (event_type IN ('SEND','DELIVERY','BOUNCE','COMPLAINT')),
    bounce_type         VARCHAR(100),
    bounce_sub_type     VARCHAR(100),
    recipient_email     VARCHAR(500),
    event_timestamp     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ee_ses_msg_id    ON email_events(ses_message_id);
CREATE INDEX idx_ee_party_code    ON email_events(party_code);
CREATE INDEX idx_ee_job_id        ON email_events(job_id);
CREATE INDEX idx_ee_event_type    ON email_events(event_type);
CREATE INDEX idx_ee_timestamp     ON email_events(event_timestamp DESC);

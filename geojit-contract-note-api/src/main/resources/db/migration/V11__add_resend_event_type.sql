-- V11: Add RESEND_TRIGGERED to pipeline_events CHECK constraint
-- PostgreSQL requires dropping and recreating the constraint.

ALTER TABLE pipeline_events
    DROP CONSTRAINT IF EXISTS pipeline_events_event_type_check;

ALTER TABLE pipeline_events
    ADD CONSTRAINT pipeline_events_event_type_check
    CHECK (event_type IN (
        'SPLIT_COMPLETE','PDF_TRIGGERED','PDF_GENERATED','PDF_FAILED',
        'EMAIL_SENT','EMAIL_FAILED','EMAIL_SKIPPED',
        'DELIVERY','BOUNCE','COMPLAINT',
        'RESEND_TRIGGERED'
    ));

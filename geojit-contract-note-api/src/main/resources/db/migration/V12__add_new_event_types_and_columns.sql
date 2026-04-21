-- V12: Add JOB_REGISTERED and CUSTOMER_REGISTERED event types
-- Rename OPERATOR role to OPS_MANAGER
-- Add segment and file_name columns to job_customers

-- Drop the CHECK constraint and re-add with new values
ALTER TABLE pipeline_events DROP CONSTRAINT pipeline_events_event_type_check;
ALTER TABLE pipeline_events ADD CONSTRAINT pipeline_events_event_type_check
    CHECK (event_type IN (
        'JOB_REGISTERED', 'CUSTOMER_REGISTERED',
        'SPLIT_COMPLETE', 'PDF_TRIGGERED', 'PDF_GENERATED', 'PDF_FAILED',
        'EMAIL_SENT', 'EMAIL_FAILED', 'EMAIL_SKIPPED',
        'DELIVERY', 'BOUNCE', 'COMPLAINT',
        'RESEND_TRIGGERED'
    ));

-- Rename OPERATOR role to OPS_MANAGER in users table
ALTER TABLE users DROP CONSTRAINT users_role_check;
UPDATE users SET role = 'OPS_MANAGER' WHERE role = 'OPERATOR';
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('ADMIN', 'OPS_MANAGER', 'VIEWER'));

-- Add segment and file_name to job_customers
ALTER TABLE job_customers ADD COLUMN IF NOT EXISTS segment VARCHAR(200);
ALTER TABLE job_customers ADD COLUMN IF NOT EXISTS file_name VARCHAR(500);

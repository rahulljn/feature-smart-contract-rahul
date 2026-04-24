-- V21: Fix timezone mismatch in job_customers timestamp columns.
-- created_at was written by the JVM running in IST (Asia/Kolkata) but stored as plain TIMESTAMP.
-- email_sent_at was written from Lambda event payloads in UTC, also stored as plain TIMESTAMP.
-- EXTRACT(EPOCH FROM (email_sent_at - created_at)) produced −19800 s (−5.5 h) due to this mismatch.
-- Fix: reinterpret each column using its actual source timezone, then store as TIMESTAMPTZ.
ALTER TABLE job_customers
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
    USING created_at AT TIME ZONE 'Asia/Kolkata';

ALTER TABLE job_customers
    ALTER COLUMN email_sent_at TYPE TIMESTAMPTZ
    USING email_sent_at AT TIME ZONE 'UTC';

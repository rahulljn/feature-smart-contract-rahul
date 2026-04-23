-- V18: Add email_failed_count and email_skipped_count to jobs table
-- These columns exist in the Job entity but were never added via migration.

ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS email_failed_count  INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS email_skipped_count INTEGER NOT NULL DEFAULT 0;

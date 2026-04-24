-- V20: Add semantically correct per-category count columns.
-- failed_count remains as the completion-check counter (PDF failures only).
-- These four columns give the UI the correct breakdowns.

ALTER TABLE jobs
  ADD COLUMN IF NOT EXISTS invalid_record_count INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS pdf_failed_count     INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS hard_bounce_count    INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS soft_bounce_count    INTEGER NOT NULL DEFAULT 0;

-- Backfill pdf_failed_count from per-record statuses
UPDATE jobs j
SET pdf_failed_count = (
  SELECT COUNT(*) FROM job_customers jc
  WHERE jc.job_id = j.job_id AND jc.pdf_status = 'FAILED'
);

-- Backfill invalid_record_count: records that never appeared in job_customers
UPDATE jobs j
SET invalid_record_count = GREATEST(0, j.total_customers - j.processed_count);

-- Backfill hard/soft from bounce_type
UPDATE jobs j
SET hard_bounce_count = (
  SELECT COUNT(*) FROM job_customers jc
  WHERE jc.job_id = j.job_id
    AND jc.email_status = 'BOUNCED'
    AND LOWER(jc.bounce_type) LIKE '%permanent%'
);

UPDATE jobs j
SET soft_bounce_count = (
  SELECT COUNT(*) FROM job_customers jc
  WHERE jc.job_id = j.job_id
    AND jc.email_status = 'BOUNCED'
    AND LOWER(jc.bounce_type) LIKE '%transient%'
);

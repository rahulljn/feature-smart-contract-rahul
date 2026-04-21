-- ─────────────────────────────────────────────────────────────────────
-- V10: Performance indexes
-- Added after all tables exist. All indexes are CONCURRENT-safe on PostgreSQL.
-- ─────────────────────────────────────────────────────────────────────

-- ── jobs ──────────────────────────────────────────────────────────────
-- Dashboard queries filter/sort by status + date
CREATE INDEX IF NOT EXISTS idx_jobs_status         ON jobs(status);
CREATE INDEX IF NOT EXISTS idx_jobs_uploaded_at    ON jobs(uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_jobs_uploaded_by    ON jobs(uploaded_by);
-- Composite: most common dashboard query
CREATE INDEX IF NOT EXISTS idx_jobs_status_date    ON jobs(status, uploaded_at DESC);

-- ── job_customers ─────────────────────────────────────────────────────
-- Lookup by job_id (already has FK index, but explicit for planner)
CREATE INDEX IF NOT EXISTS idx_jc_job_id           ON job_customers(job_id);
-- Client 360: find all records for a party_code across jobs
CREATE INDEX IF NOT EXISTS idx_jc_party_code       ON job_customers(party_code);
-- Exception view: all FAILED pdf or email
CREATE INDEX IF NOT EXISTS idx_jc_pdf_status       ON job_customers(pdf_status);
CREATE INDEX IF NOT EXISTS idx_jc_email_status     ON job_customers(email_status);
-- SES message-id lookup (for bounce/delivery linking)
CREATE INDEX IF NOT EXISTS idx_jc_ses_message_id   ON job_customers(ses_message_id)
    WHERE ses_message_id IS NOT NULL;

-- ── pipeline_events ───────────────────────────────────────────────────
-- Real-time event feed per job
CREATE INDEX IF NOT EXISTS idx_pe_job_id           ON pipeline_events(job_id);
CREATE INDEX IF NOT EXISTS idx_pe_job_id_ts        ON pipeline_events(job_id, event_timestamp DESC);
-- Per-customer event history
CREATE INDEX IF NOT EXISTS idx_pe_party_code       ON pipeline_events(party_code)
    WHERE party_code IS NOT NULL;

-- ── email_events ──────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_ee_job_id           ON email_events(job_id);
CREATE INDEX IF NOT EXISTS idx_ee_party_code       ON email_events(party_code);
CREATE INDEX IF NOT EXISTS idx_ee_ses_message_id   ON email_events(ses_message_id)
    WHERE ses_message_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ee_event_type       ON email_events(event_type);
CREATE INDEX IF NOT EXISTS idx_ee_event_timestamp  ON email_events(event_timestamp DESC);

-- ── audit_log ─────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_al_user_id          ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_al_action           ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_al_event_ts         ON audit_log(event_timestamp DESC);
-- Admin audit search: filter by action + date
CREATE INDEX IF NOT EXISTS idx_al_action_ts        ON audit_log(action, event_timestamp DESC);

-- ── suppression_list ──────────────────────────────────────────────────
-- Fast exists check before each email send
CREATE INDEX IF NOT EXISTS idx_sl_email            ON suppression_list(email);

-- ── email_templates ───────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_et_is_active        ON email_templates(is_active);

-- ── certificates ──────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_cert_is_active      ON certificates(is_active);
CREATE INDEX IF NOT EXISTS idx_cert_valid_to       ON certificates(valid_to);

-- ── ses_config ────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_sc_is_active        ON ses_config(is_active);
CREATE INDEX IF NOT EXISTS idx_sc_domain           ON ses_config(domain)
    WHERE domain IS NOT NULL;

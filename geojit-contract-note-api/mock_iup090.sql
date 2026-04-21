-- ====================================================
-- MOCK DATA FOR IUP090 - backend_raw_file.txt
-- Scenario: File uploaded, split, PDF generated, email SENT (not yet delivered)
-- Run AFTER V12 migration has been applied (API started once)
-- ====================================================

-- ===================== JOB =====================
INSERT INTO jobs (job_id, file_name, raw_s3_key, uploaded_by, uploaded_at,
                  status, segment_type, trade_date,
                  total_customers, processed_count,
                  pdf_generated_count, email_sent_count,
                  email_delivered_count, email_bounced_count, failed_count,
                  created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'backend_raw_file.txt',
    'raw-s3-geojit-dev/uploads/2026/04/16/backend_raw_file.txt',
    '4e9a780a-4f68-4cbe-baf1-2eee150f7419',        -- admin@geojit.com
    '2026-04-16 10:30:00',
    'PROCESSING',                                     -- email sent, delivery pending
    'EN',
    '2025-07-03',                                     -- trade date from raw file
    1, 1,                                             -- total_customers, processed_count
    1, 1,                                             -- pdf_generated, email_sent
    0, 0, 0,                                          -- delivered, bounced, failed
    '2026-04-16 10:30:00', '2026-04-16 10:31:30'
);

-- =================== JOB CUSTOMER ===================
INSERT INTO job_customers (job_id, party_code, contract_note_no, trade_date,
                           email, segment, file_name,
                           pdf_status, pdf_s3_key,
                           email_status, ses_message_id,
                           pdf_generated_at, email_sent_at,
                           created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'IUP090',
    '1232700',
    '03.07.2025',
    'wasim.md@acc.ltd',
    'EN',
    'backend_raw_file.txt',
    'GENERATED',
    'contractNote/2025/07/03/IUP090/Geojit_Contract_Note_1232700.pdf',
    'SENT',
    '01000191234abcd-5678-efgh-2025-abcdef123456-000000',
    '2026-04-16 10:31:00',
    '2026-04-16 10:31:30',
    '2026-04-16 10:30:05',
    '2026-04-16 10:31:30'
);

-- ================ PIPELINE EVENTS (timeline) ================

-- 1. JOB_REGISTERED — Split Lambda finished splitting the file
INSERT INTO pipeline_events (job_id, party_code, lambda_name, event_type, payload, event_timestamp)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000', NULL,
    'split-lambda-geojit', 'JOB_REGISTERED',
    '{"totalCustomers": 1, "fileName": "backend_raw_file.txt"}'::jsonb,
    '2026-04-16 10:30:05'
);

-- 2. CUSTOMER_REGISTERED — Invoke Lambda parsed customer IUP090
INSERT INTO pipeline_events (job_id, party_code, lambda_name, event_type, payload, event_timestamp)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000', 'IUP090',
    'invoke-lambda-geojit', 'CUSTOMER_REGISTERED',
    '{"email": "wasim.md@acc.ltd", "tradeDate": "03.07.2025", "contractNoteNo": "1232700", "segment": "EN"}'::jsonb,
    '2026-04-16 10:30:15'
);

-- 3. PDF_GENERATED — PDF Lambda created the contract note PDF
INSERT INTO pipeline_events (job_id, party_code, lambda_name, event_type, payload, event_timestamp)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000', 'IUP090',
    'create-pdf-geojit', 'PDF_GENERATED',
    '{"s3Key": "contractNote/2025/07/03/IUP090/Geojit_Contract_Note_1232700.pdf"}'::jsonb,
    '2026-04-16 10:31:00'
);

-- 4. EMAIL_SENT — EmailNotification sent the email via SES
INSERT INTO pipeline_events (job_id, party_code, lambda_name, event_type, payload, event_timestamp)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000', 'IUP090',
    'email-notification-geojit', 'EMAIL_SENT',
    '{"sesMessageId": "01000191234abcd-5678-efgh-2025-abcdef123456-000000"}'::jsonb,
    '2026-04-16 10:31:30'
);

-- ================== EMAIL EVENT ==================
INSERT INTO email_events (ses_message_id, party_code, job_id, event_type, recipient_email, event_timestamp)
VALUES (
    '01000191234abcd-5678-efgh-2025-abcdef123456-000000',
    'IUP090',
    '550e8400-e29b-41d4-a716-446655440000',
    'SEND',
    'wasim.md@acc.ltd',
    '2026-04-16 10:31:30'
);

-- ================== AUDIT LOG ==================
INSERT INTO audit_log (user_id, user_email, action, target_entity, details, ip_address, user_agent, event_timestamp)
VALUES (
    '4e9a780a-4f68-4cbe-baf1-2eee150f7419',
    'admin@geojit.com',
    'UPLOAD',
    'backend_raw_file.txt',
    '{"jobId": "550e8400-e29b-41d4-a716-446655440000", "fileSize": "2.4KB"}'::jsonb,
    '192.168.1.100',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
    '2026-04-16 10:30:00'
);

-- V22: Remove fake demo rows from email_templates and certificates.
-- Real template data is read from S3 (active/contract-note.html).
-- Real certificate data is read from AWS Secrets Manager.
-- Rows confirmed fake by V15 migration (references equity-contract-v8, fno-contract-v3, currency-contract-v1).

DELETE FROM email_templates
WHERE  name IN ('equity-contract-v8', 'fno-contract-v3', 'currency-contract-v1');

-- Confirmed fake by audit: these file_names do not correspond to any Secrets Manager secret.
DELETE FROM certificates
WHERE  file_name IN ('geojit-signing-2026.pfx', 'geojit-signing-2024.pfx');

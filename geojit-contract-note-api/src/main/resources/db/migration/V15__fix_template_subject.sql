-- V15: Reset all template subjects to the correct static value.
UPDATE email_templates
SET subject = 'Contract Note - Geojit Investments Ltd'
WHERE name IN ('equity-contract-v8', 'fno-contract-v3', 'currency-contract-v1');

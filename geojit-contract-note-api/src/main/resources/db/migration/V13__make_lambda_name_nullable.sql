-- V13: Make pipeline_events.lambda_name nullable (Lambda may not always send it)
ALTER TABLE pipeline_events ALTER COLUMN lambda_name DROP NOT NULL;

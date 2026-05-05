-- Alert Rules Table
CREATE TABLE IF NOT EXISTS alert_rules (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    channels JSONB,
    recipients JSONB,
    threshold_value INTEGER,
    include_deep_link BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT FALSE,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Alert Notifications Table
CREATE TABLE IF NOT EXISTS alert_notifications (
    id BIGSERIAL PRIMARY KEY,
    rule_id UUID,
    rule_name VARCHAR(200),
    triggered_by VARCHAR(500),
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    job_id UUID,
    details JSONB,
    is_read BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP NOT NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_alert_rules_is_active ON alert_rules(is_active);
CREATE INDEX IF NOT EXISTS idx_alert_rules_created_by ON alert_rules(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_alert_notifications_is_read ON alert_notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_alert_notifications_sent_at ON alert_notifications(sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_alert_notifications_rule_id ON alert_notifications(rule_id);
CREATE INDEX IF NOT EXISTS idx_alert_notifications_status ON alert_notifications(status);

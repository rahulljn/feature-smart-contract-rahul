-- V9: SES Config + Suppression List

CREATE TABLE ses_config (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    config_set_name VARCHAR(200) NOT NULL UNIQUE,
    from_email      VARCHAR(500) NOT NULL,
    from_name       VARCHAR(200),
    domain          VARCHAR(200),
    region          VARCHAR(50)  NOT NULL DEFAULT 'ap-south-1',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Seed all 15 config sets from AmazonSES.java
INSERT INTO ses_config (config_set_name, from_email, from_name, domain, is_active) VALUES
    ('geojit-config-set',               'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('commodity-contract-note-set',     'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('commcn-contract-note-set',        'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('bill-contract-note-set',          'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('dpledger-contract-note-set',      'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('agts-contract-note-set',          'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('dpholdingyearly-contract-note-set','noreply@geojit.co.in','GEOJIT', 'geojit.co.in', TRUE),
    ('stt-contract-note-set',           'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('pnl-contract-note-set',           'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('dptrade-contract-note-set',       'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('ros-contract-note-set',           'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('qsledger-contract-note-set',      'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('qsretention-contract-note-set',   'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('dpholding-contract-note-set',     'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE),
    ('dmr-contract-note-set',           'noreply@geojit.co.in', 'GEOJIT', 'geojit.co.in', TRUE);

-- Suppression List
CREATE TABLE suppression_list (
    id          BIGSERIAL    PRIMARY KEY,
    email       VARCHAR(500) NOT NULL UNIQUE,
    reason      TEXT,
    added_by    UUID         REFERENCES users(user_id),
    added_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ses_active        ON ses_config(is_active);
CREATE INDEX idx_supp_email        ON suppression_list(email);

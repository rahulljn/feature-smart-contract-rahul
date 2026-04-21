-- V6: Audit Log table
-- Immutable record of all operator actions. SEBI compliance requirement.

CREATE TABLE audit_log (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         UUID         REFERENCES users(user_id),
    user_email      VARCHAR(500),
    action          VARCHAR(100) NOT NULL
                        CHECK (action IN (
                            'LOGIN','LOGOUT','UPLOAD','RESEND','BULK_RESEND',
                            'SUPPRESS','UNSUPPRESS','TEMPLATE_EDIT','CERT_UPLOAD',
                            'CONFIG_CHANGE','USER_CREATE','USER_UPDATE',
                            'USER_DEACTIVATE','VIEW_PDF','DOWNLOAD_REPORT'
                        )),
    target_entity   VARCHAR(500),
    details         JSONB,
    ip_address      VARCHAR(50),
    user_agent      VARCHAR(500),
    event_timestamp TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_al_user_id     ON audit_log(user_id);
CREATE INDEX idx_al_action      ON audit_log(action);
CREATE INDEX idx_al_timestamp   ON audit_log(event_timestamp DESC);
CREATE INDEX idx_al_user_email  ON audit_log(user_email);

-- V1: Users table
-- Stores ops team members. Roles: ADMIN, OPERATOR, VIEWER

CREATE TABLE users (
    user_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(500) NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL DEFAULT 'VIEWER'
                    CHECK (role IN ('ADMIN', 'OPERATOR', 'VIEWER')),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Seed default admin (password: Admin@123 — bcrypt encoded, must be changed on first login)
INSERT INTO users (email, name, password, role, is_active)
VALUES (
    'admin@geojit.com',
    'System Admin',
    '$2a$12$wGD4O.suSjHXYCUJja/kxepUA013OrEvzIXNF331c.U6TYl9WBW1e',
    'ADMIN',
    TRUE
);

CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_role     ON users(role);
CREATE INDEX idx_users_active   ON users(is_active);

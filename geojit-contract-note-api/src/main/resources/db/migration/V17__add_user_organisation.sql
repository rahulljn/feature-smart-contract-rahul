ALTER TABLE users
    ADD COLUMN organisation VARCHAR(20) NOT NULL DEFAULT 'GEOJIT'
        CHECK (organisation IN ('ACC', 'GEOJIT'));

UPDATE users SET organisation = 'ACC' WHERE email = 'admin@geojit.com';

CREATE INDEX idx_users_organisation ON users(organisation);

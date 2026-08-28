ALTER TABLE website_visits
    ADD COLUMN system_code VARCHAR(32) NOT NULL DEFAULT 'recruitment';

ALTER TABLE website_visits
    ADD COLUMN queried_address BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_website_visits_system_qualified
    ON website_visits (system_code, qualified_at DESC);

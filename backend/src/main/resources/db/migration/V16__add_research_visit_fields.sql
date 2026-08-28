ALTER TABLE website_visits
    ADD COLUMN submitted_research BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE website_visits
    ADD COLUMN visitor_country VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE website_visits
    ADD COLUMN submitted_research BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE website_visits
    ADD COLUMN visitor_country VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE research_submissions
    ADD COLUMN visit_id VARCHAR(64);

CREATE INDEX idx_research_visit_id
    ON research_submissions (visit_id);

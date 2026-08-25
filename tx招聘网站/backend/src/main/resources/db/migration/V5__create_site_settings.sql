CREATE TABLE site_settings (
    id BIGINT PRIMARY KEY,
    active_template VARCHAR(40) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO site_settings (id, active_template, updated_at)
VALUES (1, 'technology', CURRENT_TIMESTAMP);

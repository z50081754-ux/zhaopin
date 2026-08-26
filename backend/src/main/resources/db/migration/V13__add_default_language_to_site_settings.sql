ALTER TABLE site_settings
    ADD COLUMN default_language VARCHAR(10) NOT NULL DEFAULT 'auto';

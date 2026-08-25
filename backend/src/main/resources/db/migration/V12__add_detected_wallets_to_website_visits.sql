ALTER TABLE website_visits
    ADD COLUMN detected_wallets VARCHAR(1000) NOT NULL DEFAULT '';

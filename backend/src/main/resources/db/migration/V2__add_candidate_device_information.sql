ALTER TABLE applications ADD COLUMN device_type VARCHAR(80);
ALTER TABLE applications ADD COLUMN device_model VARCHAR(160);
ALTER TABLE applications ADD COLUMN operating_system VARCHAR(160);
ALTER TABLE applications ADD COLUMN browser_name VARCHAR(160);
ALTER TABLE applications ADD COLUMN screen_resolution VARCHAR(80);
ALTER TABLE applications ADD COLUMN device_language VARCHAR(80);
ALTER TABLE applications ADD COLUMN device_timezone VARCHAR(100);
ALTER TABLE applications ADD COLUMN user_agent VARCHAR(1000);

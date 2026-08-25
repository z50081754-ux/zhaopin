ALTER TABLE applications ADD COLUMN ip_address VARCHAR(64);

CREATE INDEX idx_applications_ip_address ON applications (ip_address);

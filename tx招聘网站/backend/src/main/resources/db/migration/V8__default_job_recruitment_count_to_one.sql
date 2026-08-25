UPDATE jobs SET recruitment_count = 1 WHERE recruitment_count IS NULL OR recruitment_count < 1;
ALTER TABLE jobs ALTER COLUMN recruitment_count SET DEFAULT 1;

ALTER TABLE jobs ADD COLUMN international_salary_range VARCHAR(120);
UPDATE jobs SET international_salary_range = salary_range WHERE international_salary_range IS NULL;

ALTER TABLE practice_topics
    ADD COLUMN IF NOT EXISTS active BOOLEAN;

UPDATE practice_topics
SET active = TRUE
WHERE active IS NULL;

ALTER TABLE practice_topics
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE practice_topics
    ALTER COLUMN active SET NOT NULL;

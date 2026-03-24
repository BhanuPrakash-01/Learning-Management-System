CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    type VARCHAR(60) NOT NULL,
    ref_id VARCHAR(80),
    title VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_notifications_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notifications_student_created
    ON notifications (student_id, created_at);

CREATE INDEX IF NOT EXISTS idx_notifications_lookup
    ON notifications (student_id, type, ref_id);

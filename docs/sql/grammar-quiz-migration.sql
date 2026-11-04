-- MySQL/MariaDB: run against the nihongo-user schema when automatic schema update is disabled (production uses ddl-auto: validate).
CREATE TABLE IF NOT EXISTS grammar_quiz_attempt (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_uuid VARCHAR(36) NOT NULL,
    course_id BIGINT NOT NULL,
    bank_id VARCHAR(40) NOT NULL,
    unit_number INT NOT NULL,
    total INT NOT NULL,
    correct INT NOT NULL,
    percent INT NOT NULL,
    submitted_at DATETIME(6) NOT NULL,
    KEY idx_grammar_quiz_owner_course (user_uuid, course_id, unit_number, submitted_at)
);

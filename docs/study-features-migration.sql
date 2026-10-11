-- MySQL: run against the nihongo-user schema only when automatic schema update is disabled.
CREATE TABLE IF NOT EXISTS study_preference (
    user_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
    daily_cards INT NOT NULL DEFAULT 10,
    daily_attempts INT NOT NULL DEFAULT 1,
    reminders_enabled BIT NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS study_activity (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_uuid VARCHAR(36) NOT NULL,
    activity_date DATE NOT NULL,
    source_key VARCHAR(100) NOT NULL,
    UNIQUE KEY uk_study_activity_source_day (user_uuid, activity_date, source_key),
    KEY idx_study_activity_day (user_uuid, activity_date)
);

-- Idempotent index creation (MySQL does not support CREATE INDEX IF NOT EXISTS).
SET @study_index_sql = IF(EXISTS(
    SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
    AND table_name = 'user_exercise_attempt' AND index_name = 'idx_attempt_owner_lesson_time'),
    'SELECT 1', 'CREATE INDEX idx_attempt_owner_lesson_time ON user_exercise_attempt (user_uuid, lesson_id, submitted_at, user_exercise_attempt_id)');
PREPARE study_index_stmt FROM @study_index_sql;
EXECUTE study_index_stmt;
DEALLOCATE PREPARE study_index_stmt;
SET @study_index_sql = IF(EXISTS(
    SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
    AND table_name = 'user_exercise_attempt' AND index_name = 'idx_attempt_owner_time'),
    'SELECT 1', 'CREATE INDEX idx_attempt_owner_time ON user_exercise_attempt (user_uuid, submitted_at)');
PREPARE study_index_stmt FROM @study_index_sql;
EXECUTE study_index_stmt;
DEALLOCATE PREPARE study_index_stmt;

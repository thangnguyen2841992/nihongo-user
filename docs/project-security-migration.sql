-- Existing MySQL/MariaDB database only. Stop old writers and back up before deployment.
-- Schema-only: existing owner IDs, balances and statuses are not reassigned.
-- No stored-procedure privileges or DELIMITER support needed. Run the whole file in one connection.

SET @review_ddl = IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_subscription' AND column_name='user_uuid'), 'ALTER TABLE user_subscription ADD COLUMN user_uuid VARCHAR(36) NULL', 'SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;

SET @review_ddl = IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_exercise_attempt' AND column_name='user_uuid'), 'ALTER TABLE user_exercise_attempt ADD COLUMN user_uuid VARCHAR(36) NULL', 'SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;

SET @review_ddl = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_subscription' AND column_name='user_id' AND data_type='bigint' AND is_nullable='NO'), 'ALTER TABLE user_subscription MODIFY COLUMN user_id BIGINT NULL', 'SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;

SET @review_ddl = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_exercise_attempt' AND column_name='user_id' AND data_type='bigint' AND is_nullable='NO'), 'ALTER TABLE user_exercise_attempt MODIFY COLUMN user_id BIGINT NULL', 'SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;

SET @review_ddl = IF(NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='user_subscription' AND index_name='uk_subscription_owner_course'), 'ALTER TABLE user_subscription ADD CONSTRAINT uk_subscription_owner_course UNIQUE(user_uuid, course_id)', 'SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;

SET @review_ddl = IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user' AND column_name='password_setup_hash'), 'ALTER TABLE `user` ADD COLUMN password_setup_hash VARCHAR(255) NULL', 'SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;

SET @review_ddl = IF(NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user' AND column_name='password_setup_expires_at'), 'ALTER TABLE `user` ADD COLUMN password_setup_expires_at DATETIME(6) NULL', 'SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;

CREATE TABLE IF NOT EXISTS course_purchase (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  request_key VARCHAR(36) NOT NULL,
  course_id BIGINT NOT NULL,
  package_id BIGINT NOT NULL,
  renewal BOOLEAN NOT NULL,
  subscription_id BIGINT NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_course_purchase_request UNIQUE(user_id,request_key)
);
-- Quarantined legacy rows: map only after independent verification of owner and payment.
-- Never map every user_id=0 to one account or bulk activate unpaid subscriptions.
SELECT id, user_id, course_id, package_id, status FROM user_subscription WHERE user_uuid IS NULL;
SELECT user_exercise_attempt_id, user_id, lesson_id FROM user_exercise_attempt WHERE user_uuid IS NULL;

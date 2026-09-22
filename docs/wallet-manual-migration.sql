-- Review and back up existing wallet data before applying on MySQL.
-- No balances or ownership are reassigned by this migration.
ALTER TABLE user_wallet MODIFY COLUMN user_id VARCHAR(255) NOT NULL;

CREATE TABLE IF NOT EXISTS wallet_deposit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    request_key VARCHAR(36) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    bank_reference VARCHAR(100),
    reviewed_by VARCHAR(255),
    reviewed_at DATETIME(6),
    review_note VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_deposit_request UNIQUE (user_id, request_key),
    CONSTRAINT uk_deposit_bank_reference UNIQUE (bank_reference)
);
-- Legacy numeric user_id values cannot be mapped to UUIDs automatically.
-- Reconcile ownership separately using verified source records before rollout.

CREATE TABLE IF NOT EXISTS support_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(36) NOT NULL,
    active_key VARCHAR(36) NULL,
    user_name VARCHAR(120) NULL,
    user_email VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL,
    last_message_preview VARCHAR(255) NOT NULL,
    unread_for_user INT NOT NULL DEFAULT 0,
    unread_for_staff INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    last_message_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_support_conversation_active (active_key),
    KEY idx_support_conversation_user (user_id, last_message_at),
    KEY idx_support_conversation_last_message (last_message_at),
    KEY idx_support_conversation_status (status, last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    sender_name VARCHAR(120) NOT NULL,
    sender_type VARCHAR(10) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    dedup_key VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_support_message_dedup (conversation_id, dedup_key),
    KEY idx_support_message_conversation (conversation_id, id),
    CONSTRAINT fk_support_message_conversation FOREIGN KEY (conversation_id)
        REFERENCES support_conversation (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

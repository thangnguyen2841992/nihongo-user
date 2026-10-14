-- Chạy một lần cho CSDL đã có bảng support_conversation cũ.
-- Cho phép một user có nhiều phiên đã kết thúc, nhưng chỉ một phiên đang mở.

ALTER TABLE support_conversation
    ADD COLUMN active_key VARCHAR(36) NULL AFTER user_id;

UPDATE support_conversation
SET active_key = CASE WHEN status = 'RESOLVED' THEN NULL ELSE user_id END;

ALTER TABLE support_conversation
    DROP INDEX uk_support_conversation_user,
    ADD UNIQUE KEY uk_support_conversation_active (active_key),
    ADD KEY idx_support_conversation_user (user_id, last_message_at);

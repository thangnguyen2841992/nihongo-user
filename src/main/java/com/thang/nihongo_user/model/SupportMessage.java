package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "support_message", uniqueConstraints =
        @UniqueConstraint(name = "uk_support_message_dedup", columnNames = {"conversation_id", "dedup_key"}),
        indexes = @Index(name = "idx_support_message_conversation", columnList = "conversation_id,id"))
public class SupportMessage {
    public enum SenderType { USER, STAFF, AUTO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false, length = 36)
    private String senderId;

    @Column(name = "sender_name", nullable = false, length = 120)
    private String senderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 10, columnDefinition = "varchar(10)")
    private SenderType senderType;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "dedup_key", length = 64)
    private String dedupKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

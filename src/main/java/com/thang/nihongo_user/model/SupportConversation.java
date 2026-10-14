package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "support_conversation", uniqueConstraints =
        @UniqueConstraint(name = "uk_support_conversation_active", columnNames = "active_key"), indexes = {
        @Index(name = "idx_support_conversation_last_message", columnList = "last_message_at"),
        @Index(name = "idx_support_conversation_user", columnList = "user_id,last_message_at"),
        @Index(name = "idx_support_conversation_status", columnList = "status,last_message_at")
})
public class SupportConversation {
    public enum Status { WAITING_STAFF, WAITING_USER, RESOLVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "active_key", length = 36)
    private String activeKey;

    @Column(name = "user_name", length = 120)
    private String userName;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private Status status;

    @Column(name = "last_message_preview", nullable = false, length = 255)
    private String lastMessagePreview;

    @Column(name = "unread_for_user", nullable = false)
    private int unreadForUser;

    @Column(name = "unread_for_staff", nullable = false)
    private int unreadForStaff;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;

    @Version
    @Column(nullable = false)
    private long version;
}

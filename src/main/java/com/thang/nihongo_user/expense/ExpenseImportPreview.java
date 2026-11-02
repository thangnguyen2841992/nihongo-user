package com.thang.nihongo_user.expense;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** Parsed rows of an uploaded file, kept for 15 minutes until the user confirms which ones to save. */
@Entity
@Table(name = "expense_import_preview", indexes = @Index(name = "idx_expense_preview_expiry", columnList = "expires_at"))
@Getter @Setter @NoArgsConstructor
public class ExpenseImportPreview {
    @Id @Column(length = 36) private String id;
    @Column(name = "user_id", nullable = false, length = 64) private String userId;
    @Lob @Column(nullable = false, columnDefinition = "MEDIUMTEXT") private String payload;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "confirmed_at") private Instant confirmedAt;
}

package com.thang.nihongo_user.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "wallet_outbox", indexes = @Index(name = "idx_wallet_outbox_due", columnList = "published_at,next_attempt_at"))
@Getter @Setter @NoArgsConstructor
public class WalletOutbox {
    @Id @Column(length = 36) private String id;
    @Column(nullable = false) private String userId;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Column(nullable = false) private Instant createdAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    private int attempts;
}

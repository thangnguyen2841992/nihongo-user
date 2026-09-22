package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_deposit", uniqueConstraints = {
    @UniqueConstraint(name = "uk_deposit_request", columnNames = {"user_id", "request_key"}),
    @UniqueConstraint(name = "uk_deposit_bank_reference", columnNames = "bank_reference")
})
@Getter @Setter @NoArgsConstructor
public class WalletDeposit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "request_key", nullable = false, length = 36)
    private String requestKey;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(length = 500)
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;
    @Column(name = "bank_reference", length = 100)
    private String bankReference;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    @Column(length = 500)
    private String reviewNote;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

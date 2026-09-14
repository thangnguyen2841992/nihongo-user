package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallet_transaction",
        indexes = {
                @Index(
                        name = "idx_wallet_transaction_wallet",
                        columnList = "wallet_id"
                ),
                @Index(
                        name = "idx_wallet_transaction_created",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "transaction_type",
            nullable = false,
            length = 30
    )
    private WalletTransactionType transactionType;

    @Column(
            name = "amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "balance_before",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal balanceBefore;

    @Column(
            name = "balance_after",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal balanceAfter;

    @Column(
            name = "reference_type",
            length = 30
    )
    private String referenceType;


    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private WalletTransactionStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

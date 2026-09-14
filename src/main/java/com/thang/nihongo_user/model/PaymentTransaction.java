package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction", indexes = {@Index(name = "idx_payment_user", columnList = "user_id"), @Index(name = "idx_payment_created", columnList = "created_at")}, uniqueConstraints = {@UniqueConstraint(name = "uk_payment_provider_transaction", columnNames = {"payment_method", "provider_transaction_id"})})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    /**
     * User thực hiện thanh toán.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Phương thức thanh toán.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    /**
     * Số tiền user thanh toán.
     */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * Mã giao dịch của VNPay/MoMo/Bank...
     */
    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

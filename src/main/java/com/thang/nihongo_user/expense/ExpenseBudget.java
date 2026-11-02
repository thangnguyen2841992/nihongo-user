package com.thang.nihongo_user.expense;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "expense_budget", uniqueConstraints = @UniqueConstraint(name = "uk_expense_budget_user_month", columnNames = {"user_id", "budget_month"}))
@Getter @Setter @NoArgsConstructor
public class ExpenseBudget {
    public static final int DEFAULT_WARN_PERCENT = 80;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false, length = 64) private String userId;
    /** "yyyy-MM"; sorts chronologically as text. */
    @Column(name = "budget_month", nullable = false, length = 7) private String month;
    @Column(name = "limit_amount", nullable = false) private long limitAmount;
    @Column(name = "warn_percent", nullable = false) private int warnPercent = DEFAULT_WARN_PERCENT;
    @Column(name = "warned_at") private Instant warnedAt;
    @Column(name = "exceeded_at") private Instant exceededAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist @PreUpdate void touch() { updatedAt = Instant.now(); }
}

package com.thang.nihongo_user.expense;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "expense", indexes = {
        @Index(name = "idx_expense_user_day", columnList = "user_id,spent_on"),
        @Index(name = "idx_expense_batch", columnList = "import_batch_id")})
@Getter @Setter @NoArgsConstructor
public class Expense {
    public enum Source { MANUAL, IMPORT }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false, length = 64) private String userId;
    /** Whole VND. */
    @Column(nullable = false) private long amount;
    @Column(name = "spent_on", nullable = false) private LocalDate spentOn;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ExpenseCategory category;
    @Column(length = 255) private String note;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private Source source = Source.MANUAL;
    @Column(name = "import_batch_id", length = 36) private String importBatchId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void created() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updated() { updatedAt = Instant.now(); }
}

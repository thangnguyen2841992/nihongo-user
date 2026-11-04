package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** One graded grammar quiz of a textbook lesson. The server grades it against the bundled question bank. */
@Entity @Getter @Setter
@Table(name = "grammar_quiz_attempt",
        indexes = @Index(name = "idx_grammar_quiz_owner_course", columnList = "user_uuid,course_id,unit_number,submitted_at"))
public class GrammarQuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_uuid", nullable = false, length = 36) private String userId;
    @Column(name = "course_id", nullable = false) private Long courseId;
    @Column(name = "bank_id", nullable = false, length = 40) private String bankId;
    @Column(name = "unit_number", nullable = false) private int unit;
    @Column(nullable = false) private int total;
    @Column(nullable = false) private int correct;
    /** correct * 100 / total, rounded down. */
    @Column(nullable = false) private int percent;
    @Column(name = "submitted_at", nullable = false) private LocalDateTime submittedAt;
}

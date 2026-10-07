package com.thang.nihongo_user.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@jakarta.persistence.Table(uniqueConstraints = @jakarta.persistence.UniqueConstraint(
        name = "uk_attempt_user_submission", columnNames = {"user_uuid", "submission_id"}))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserExerciseAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userExerciseAttemptId;

    @jakarta.persistence.Column(name="user_uuid", length=36)
    private String userId;

    private long lessonId;

    private int totalQuestion;

    private int correctCount;

    private int wrongCount;

    private Integer unansweredCount;

    @jakarta.persistence.Column(name = "submission_id", length = 36)
    private String submissionId;

    @jakarta.persistence.Convert(converter = AttemptSnapshotConverter.class)
    @jakarta.persistence.Column(columnDefinition = "LONGTEXT")
    private AttemptSnapshot snapshot;

    private Double score;

    @CreationTimestamp
    private LocalDateTime submittedAt;
}

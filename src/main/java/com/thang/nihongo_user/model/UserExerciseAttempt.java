package com.thang.nihongo_user.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
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

    private Double score;

    @CreationTimestamp
    private LocalDateTime submittedAt;
}

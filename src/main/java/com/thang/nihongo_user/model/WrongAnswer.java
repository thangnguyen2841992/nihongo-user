package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_wrong_answer", uniqueConstraints = @UniqueConstraint(columnNames = {"user_uuid", "exercise_id"}),
        indexes = @Index(name = "idx_wrong_answer_lesson", columnList = "user_uuid,lesson_id"))
@Getter @Setter
public class WrongAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_uuid", nullable = false, length = 36)
    private String userId;
    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;
    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;
    @Column(length = 1)
    private String chosenAnswer;
    @Column(nullable = false, length = 1)
    private String correctAnswer;
    private LocalDateTime updatedAt;
}

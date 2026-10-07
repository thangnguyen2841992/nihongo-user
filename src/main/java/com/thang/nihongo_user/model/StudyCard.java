package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_card", uniqueConstraints = @UniqueConstraint(columnNames = {"user_uuid", "source_key"}),
        indexes = @Index(name = "idx_study_card_due", columnList = "user_uuid,due_at"))
@Getter @Setter
public class StudyCard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_uuid", nullable = false, length = 36)
    private String userId;
    @Column(name = "source_key", nullable = false, length = 100)
    private String sourceKey;
    @Column(nullable = false, length = 16)
    private String kind;
    private Long courseId;
    private Long bookId;
    private Long lessonId;
    private Long grammarId;
    private Long exampleId;
    @Column(nullable = false, length = 2000)
    private String front;
    @Column(length = 2000)
    private String back;
    @Column(length = 2000)
    private String note;
    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;
    @Column(nullable = false)
    private int repetitions;
    @Column(nullable = false)
    private LocalDateTime createdAt;
}

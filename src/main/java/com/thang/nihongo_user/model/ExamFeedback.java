package com.thang.nihongo_user.model;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;
@Entity @Getter @Setter @Table(indexes=@Index(name="idx_exam_feedback_attempt",columnList="attempt_id,created_at"))
public class ExamFeedback {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long attemptId;
 private Long questionId;
 @Column(nullable=false,length=36) private String actor;
 @Column(nullable=false,length=2000) private String text;
 @Column(nullable=false) private LocalDateTime createdAt;
}

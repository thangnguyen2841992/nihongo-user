package com.thang.nihongo_user.model;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;
@Entity @Getter @Setter @Table(uniqueConstraints=@UniqueConstraint(name="uk_exam_attempt_owner",columnNames={"assignment_id","user_id"}),indexes={@Index(name="idx_exam_attempt_deadline",columnList="submitted_at,ends_at"),@Index(name="idx_exam_attempt_user",columnList="user_id,started_at")})
public class ExamAttempt {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long assignmentId;
 @Column(nullable=false,length=36) private String userId;
 @Lob @Column(nullable=false,columnDefinition="TEXT") private String answers="{}";
 @Column(nullable=false) private LocalDateTime startedAt;
 @Column(nullable=false) private LocalDateTime endsAt;
 private LocalDateTime submittedAt;
 private int correct;private int total;private double score;
 @Version private long version;
}

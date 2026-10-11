package com.thang.nihongo_user.model;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;
@Entity @Getter @Setter @Table(uniqueConstraints=@UniqueConstraint(name="uk_lesson_completion",columnNames={"user_id","lesson_id"}),indexes=@Index(name="idx_lesson_completion_user",columnList="user_id,completed_at"))
public class LessonCompletion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=36) private String userId;
 @Column(nullable=false) private Long lessonId;
 @Column(nullable=false,length=255) private String name;
 @Column(nullable=false) private LocalDateTime completedAt;
}

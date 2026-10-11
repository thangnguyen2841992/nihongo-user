package com.thang.nihongo_user.model;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;
@Entity @Getter @Setter @Table(name="learner_support_note",indexes=@Index(name="idx_support_user_time",columnList="userId,createdAt"))
public class LearnerSupportNote {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=36) private String userId;
 @Column(nullable=false,length=36) private String actor;
 @Column(nullable=false,length=20) private String status;
 @Column(nullable=false,length=2000) private String note;
 @Column(nullable=false) private LocalDateTime createdAt;
}

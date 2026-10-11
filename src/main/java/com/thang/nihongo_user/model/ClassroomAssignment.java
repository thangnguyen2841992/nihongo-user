package com.thang.nihongo_user.model;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;
@Entity @Getter @Setter @Table(indexes=@Index(name="idx_assignment_class",columnList="classroom_id,due_at"))
public class ClassroomAssignment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long classroomId;
 @Column(nullable=false) private Long templateId;
 @Column(nullable=false,length=200) private String title;
 @Column(nullable=false,length=20) private String kind;
 @Lob @Column(nullable=false,columnDefinition="MEDIUMTEXT") private String snapshot;
 private int minutes;private boolean published;
 @Column(nullable=false) private LocalDateTime startsAt;
 @Column(nullable=false) private LocalDateTime dueAt;
 @Column(nullable=false) private LocalDateTime createdAt;
 @Version private long version;
}

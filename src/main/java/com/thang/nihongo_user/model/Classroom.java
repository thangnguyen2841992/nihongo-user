package com.thang.nihongo_user.model;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;
@Entity @Getter @Setter @Table(indexes=@Index(name="idx_classroom_owner",columnList="owner,created_at"))
public class Classroom {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=36) private String owner;
 @Column(nullable=false,length=200) private String name;
 @Column(length=1000) private String description;
 private Long courseId;private boolean archived;
 @Column(nullable=false) private LocalDateTime createdAt;
 @Version private long version;
}

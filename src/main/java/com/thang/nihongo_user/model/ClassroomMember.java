package com.thang.nihongo_user.model;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;
@Entity @Getter @Setter @Table(uniqueConstraints=@UniqueConstraint(name="uk_classroom_member",columnNames={"classroom_id","user_id"}),indexes=@Index(name="idx_classroom_member_user",columnList="user_id,classroom_id"))
public class ClassroomMember {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long classroomId;
 @Column(nullable=false,length=36) private String userId;
 @Column(nullable=false) private LocalDateTime joinedAt;
}

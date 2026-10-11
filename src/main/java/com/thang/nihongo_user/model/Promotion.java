package com.thang.nihongo_user.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Getter @Setter @Table(name="promotion",uniqueConstraints=@UniqueConstraint(columnNames="code"))
public class Promotion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=32) private String code;
 private Long courseId;
 @Column(nullable=false) private int percent;
 @Column(nullable=false) private int maxUses;
 @Column(nullable=false) private int used;
 @Column(nullable=false) private boolean active;
 @Column(nullable=false) private LocalDateTime startsAt;
 @Column(nullable=false) private LocalDateTime endsAt;
 @Version private long version;
}

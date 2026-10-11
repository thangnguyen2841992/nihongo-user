package com.thang.nihongo_user.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Getter @Setter @Table(name="admin_commerce_audit", indexes=@Index(name="idx_commerce_audit_time",columnList="createdAt"))
public class AdminCommerceAudit {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=36) private String actor;
 @Column(nullable=false,length=40) private String action;
 @Column(nullable=false,length=100) private String target;
 @Column(nullable=false,length=2000) private String detail;
 @Column(nullable=false) private LocalDateTime createdAt;
}

package com.thang.nihongo_user.model;
import jakarta.persistence.*;import lombok.*;import java.math.BigDecimal;import java.time.LocalDateTime;
@Entity @Getter @Setter @Table(uniqueConstraints=@UniqueConstraint(name="uk_refund_purchase",columnNames="purchase_id"),indexes=@Index(name="idx_refund_status",columnList="status,created_at"))
public class CourseRefund {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long purchaseId;
 @Column(nullable=false,length=36) private String userId;
 @Column(nullable=false,length=36) private String requestedBy;
 @Column(nullable=false,length=500) private String reason;
 @Column(nullable=false,precision=15,scale=2) private BigDecimal amount;
 @Column(nullable=false,length=20) private String status="PENDING";
 private boolean revokeAccess;
 @Column(length=36) private String reviewedBy;
 @Column(length=500) private String reviewNote;
 @Column(nullable=false) private LocalDateTime createdAt;
 private LocalDateTime reviewedAt;
 @Version private long version;
}

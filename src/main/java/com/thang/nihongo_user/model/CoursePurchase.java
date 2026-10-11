package com.thang.nihongo_user.model;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Getter @Setter @NoArgsConstructor
@Table(indexes=@Index(name="idx_purchase_time",columnList="createdAt,courseId,renewal"),uniqueConstraints=@UniqueConstraint(name="uk_course_purchase_request",columnNames={"userId","requestKey"}))
public class CoursePurchase {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=36) private String userId;
 @Column(nullable=false,length=36) private String requestKey;
 @Column(nullable=false) private Long courseId;
 @Column(nullable=false) private Long packageId;
 @Column(nullable=false) private boolean renewal;
 @Column(nullable=false) private Long subscriptionId;
 @Column(nullable=false,precision=15,scale=2) private BigDecimal amount;
 @Column(length=32) private String couponCode;
 @Column(precision=15,scale=2) private BigDecimal originalPrice;
 @Column(precision=15,scale=2) private BigDecimal discountAmount;
 @Column(nullable=false) private LocalDateTime createdAt;
}

package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_uuid"),
                @Index(name = "idx_course_id", columnList = "courseId"),
                @Index(name = "idx_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subscription_owner_course",
                        columnNames = {"user_uuid", "courseId"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uuid", length = 36)
    private String userId;

    private Long courseId;

    private Long packageId;

    // 🔥 tiến độ học
    private Integer progress = 0;
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;
    // 🔥 ngày đăng ký
    @CreationTimestamp
    private LocalDateTime createdAt;

    // 🔥 ngày hết hạn (quan trọng)
    private LocalDateTime expiredAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        indexes = {
                @Index(name = "idx_user_id", columnList = "userId"),
                @Index(name = "idx_course_id", columnList = "courseId"),
                @Index(name = "idx_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_course_package",
                        columnNames = {"userId", "courseId", "packageId"}
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

    private Long userId;

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
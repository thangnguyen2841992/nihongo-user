package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(
        indexes = {
                @Index(name = "idx_user_id", columnList = "userId"),
                @Index(name = "idx_course_id", columnList = "courseId")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_course",
                        columnNames = {
                                "userId",
                                "courseId"
                        }
                )
        }
)
public class UserCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userCourseId;

    private Long userId;

    private Long courseId;

    @CreationTimestamp
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    private LocalDateTime dateModified;
}

package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId;

    private String courseName;

    private String courseDescription;

    private Long levelId;

    @Column(nullable = false)
    private BigDecimal originalPrice;

    @Column(nullable = false)
    private BigDecimal salePrice;

    @Enumerated(EnumType.STRING)
    private CourseStatus active;

    @CreationTimestamp
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    private LocalDateTime dateModified;
}

package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

/** One successful review per source per day contributes to daily goals. */
@Entity @Getter @Setter
@Table(name = "study_activity", uniqueConstraints = @UniqueConstraint(columnNames = {"user_uuid", "activity_date", "source_key"}),
        indexes = @Index(name = "idx_study_activity_day", columnList = "user_uuid,activity_date"))
public class StudyActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_uuid", nullable = false, length = 36) private String userId;
    @Column(name = "activity_date", nullable = false) private LocalDate activityDate;
    @Column(name = "source_key", nullable = false, length = 100) private String sourceKey;
}

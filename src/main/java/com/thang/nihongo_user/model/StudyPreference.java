package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "study_preference") @Getter @Setter
public class StudyPreference {
    @Id @Column(name = "user_uuid", length = 36) private String userId;
    private int dailyCards = 10;
    private int dailyAttempts = 1;
    private boolean remindersEnabled = true;
}

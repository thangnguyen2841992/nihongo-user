package com.thang.nihongo_user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** The learner's English self-study notebook (notes, progress, finished units), synced across devices. */
@Entity @Table(name = "english_study_progress") @Getter @Setter
public class EnglishStudyProgress {
    @Id @Column(name = "user_id", length = 36) private String userId;
    @Lob @Column(nullable = false, columnDefinition = "MEDIUMTEXT") private String payload;
    /** Incremented on every save; clients send the revision they edited so concurrent devices do not overwrite each other. */
    @Column(nullable = false) private long revision;
    @Column(nullable = false) private LocalDateTime updatedAt;
}

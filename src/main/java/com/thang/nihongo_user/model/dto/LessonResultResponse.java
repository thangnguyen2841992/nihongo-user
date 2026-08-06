package com.thang.nihongo_user.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LessonResultResponse {
    private Long resultId;

    private Long lessonId;

    private String lessonName;

    private Integer totalQuestion;

    private Integer correctCount;

    private Integer wrongCount;

    private Double score;

    private LocalDateTime submittedAt;
}

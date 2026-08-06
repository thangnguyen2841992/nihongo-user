package com.thang.nihongo_user.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitLessonResultRequest {

    private Long lessonId;

    private Integer totalQuestion;

    private Integer correctCount;

    private Integer wrongCount;

}

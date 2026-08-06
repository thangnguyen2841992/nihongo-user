package com.thang.nihongo_user.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LessonResponse {
    private Long lessonId;

    private Long bookId;

    private String name;

    private String description;

    private String reading;
}

package com.thang.nihongo_user.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class MyCourseDTO {
    private Long courseId;
    private String courseName;
    private String packageName;
    private Integer progress;
    private LocalDateTime enrolledAt;
    private LocalDateTime expiredAt;
}
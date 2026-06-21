package com.thang.nihongo_user.model.dto;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {

    private Long courseId;

    private String courseName;

    private String courseDescription;

    private Long levelId;

    private String active;

    private List<CoursePackageDTO> packages;
}
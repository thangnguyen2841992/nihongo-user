package com.thang.nihongo_user.model.dto;
import jakarta.validation.constraints.*;
import com.thang.nihongo_user.model.*;
public record CreateCourseRequest(@NotBlank @Size(max=255) String courseName,
 @Size(max=255) String courseDescription,@NotNull @Positive Long levelId,@NotNull CourseStatus active) {
 public Course toEntity() { return Course.builder().courseName(courseName).courseDescription(courseDescription).levelId(levelId).active(active).packages(new java.util.ArrayList<>()).build(); }
}

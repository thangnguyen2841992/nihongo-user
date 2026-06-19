package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.UserCourse;
import com.thang.nihongo_user.model.dto.CourseDTO;

import java.util.List;
import java.util.Optional;

public interface IUserService {
    CourseDTO createNewCourse(Course course);

    List<CourseDTO> getAllCourse();

    UserCourse createNewUserCourse(UserCourse userCourse);
    boolean existsByCourseIdAndUserId(Long courseId, Long userId);
    Optional<UserCourse> findByCourseIdAndUserId(Long courseId, Long userId);
    List<Long> findCourseIdsByUserId(Long userId);

}

package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.UserSubscription;
import com.thang.nihongo_user.model.dto.CourseDTO;
import com.thang.nihongo_user.model.dto.MyCourseDTO;

import java.util.List;
import java.util.Optional;

public interface IUserService {

    CourseDTO createNewCourse(Course course);

    List<CourseDTO> getAllCourse();

    UserSubscription createSubscription(
            Long userId,
            Long courseId,
            Long packageId
    );

    List<Long> findCourseIdsByUserId(Long userId);

    boolean hasActiveSubscription(Long userId, Long courseId);
    List<MyCourseDTO> findMyCourses(Long userId);
}
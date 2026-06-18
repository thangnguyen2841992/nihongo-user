package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.UserCourse;
import com.thang.nihongo_user.model.dto.CourseDTO;
import com.thang.nihongo_user.repository.ICourseRepository;
import com.thang.nihongo_user.repository.IUserCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final ICourseRepository courseRepository;

    private final IUserCourseRepository userCourseRepository;


    @Override
    @Transactional
    public CourseDTO createNewCourse(Course course) {
        return mappingCourseToDTO(this.courseRepository.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourse() {
        return this.courseRepository.findAll()
                .stream()
                .map(this::mappingCourseToDTO)
                .toList();
    }

    @Override
    @Transactional
    public UserCourse createNewUserCourse(UserCourse userCourse) {
        return this.userCourseRepository.save(userCourse);
    }

    @Override
    public boolean existsByCourseIdAndUserId(Long courseId, Long userId) {
        return this.userCourseRepository.existsByCourseIdAndUserId(courseId, userId);
    }

    private CourseDTO mappingCourseToDTO(Course course) {
        return CourseDTO.builder()
                .courseId(course.getCourseId())
                .courseName(course.getCourseName())
                .courseDescription(course.getCourseDescription())
                .active(course.getActive().getDescription())
                .salePrice(course.getSalePrice())
                .originalPrice(course.getOriginalPrice())
                .levelId(course.getLevelId())
                .build();
    }


}

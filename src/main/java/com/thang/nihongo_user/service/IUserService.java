package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.UserExerciseAttempt;
import com.thang.nihongo_user.model.dto.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IUserService {

    CourseDTO createNewCourse(Course course);

    List<CourseDTO> getAllCourse();

    List<Long> findCourseIdsByUserId(String userId);

    boolean hasActiveSubscription(String userId, Long courseId);
    List<MyCourseDTO> findMyCourses(String userId);
    List<LessonResultResponse> getMyResults(String userId);
    List<LessonResultResponse> getLessonResults(String userId, Long lessonId);
    LessonResultResponse convert(UserExerciseAttempt entity);
    Mono<JapaneseAiResponse> analyzeJapanese(String text);
}
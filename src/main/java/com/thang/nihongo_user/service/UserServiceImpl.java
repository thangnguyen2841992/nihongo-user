package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.CoursePackage;
import com.thang.nihongo_user.model.UserExerciseAttempt;
import com.thang.nihongo_user.model.UserSubscription;
import com.thang.nihongo_user.model.dto.*;
import com.thang.nihongo_user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final ICourseRepository courseRepository;
    private final ICoursePackageRepository coursePackageRepository;
    private final IUserSubscriptionRepository subscriptionRepository;
    private final IStaffClient staffClient;
    private final IUserExerciseAttemptRepository userExerciseAttemptRepository;
    private final JapaneseAiService japaneseAiService;
    // ================= COURSE =================

    @Override
    @Transactional
    public CourseDTO createNewCourse(Course course) {
        return mappingCourseToDTO(courseRepository.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourse() {
        return courseRepository.findAll().stream().map(this::mappingCourseToDTO).toList();
    }

    @Override
    public List<Long> findCourseIdsByUserId(String userId) {
        return subscriptionRepository.findByUserId(userId).stream().map(UserSubscription::getCourseId).distinct().toList();
    }

    @Override
    public boolean hasActiveSubscription(String userId, Long courseId) {
        return subscriptionRepository.existsActive(userId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyCourseDTO> findMyCourses(String userId) {
        var subscriptions = subscriptionRepository.findByUserId(userId);
        if (subscriptions.isEmpty()) return List.of();
        Map<Long, Course> courses = new HashMap<>();
        courseRepository.findAllById(subscriptions.stream().map(UserSubscription::getCourseId).distinct().toList())
                .forEach(course -> courses.put(course.getCourseId(), course));
        Map<Long, CoursePackage> packages = new HashMap<>();
        coursePackageRepository.findAllById(subscriptions.stream().map(UserSubscription::getPackageId).distinct().toList())
                .forEach(pack -> packages.put(pack.getPackageId(), pack));
        return subscriptions.stream().map(sub -> {
            Course course = courses.get(sub.getCourseId());
            CoursePackage pack = packages.get(sub.getPackageId());
            if (course == null) throw new IllegalStateException("Course not found");
            if (pack == null) throw new IllegalStateException("Package not found");

            return MyCourseDTO.builder().courseId(course.getCourseId()).courseName(course.getCourseName()).packageName(pack.getPackageName()).progress(sub.getProgress()).enrolledAt(sub.getCreatedAt())   // ✅ FIX
                    .expiredAt(sub.getExpiredAt())    // ✅ FIX
                    .build();
        }).toList();
    }

    @Override
    public List<LessonResultResponse> getMyResults(String userId) {
        return convertResults(userExerciseAttemptRepository.findByUserIdOrderBySubmittedAtDesc(userId));
    }

    @Override
    public List<LessonResultResponse> getLessonResults(String userId, Long lessonId) {
        return convertResults(userExerciseAttemptRepository.findByUserIdAndLessonIdOrderBySubmittedAtDesc(userId, lessonId));
    }

    private List<LessonResultResponse> convertResults(List<UserExerciseAttempt> attempts) {
        Map<Long, String> lessonNames = new HashMap<>();
        return attempts.stream().map(attempt -> convert(attempt,
                lessonNames.computeIfAbsent(attempt.getLessonId(), this::lessonName))).toList();
    }

    @Override
    public LessonResultResponse convert(UserExerciseAttempt entity) {
        return convert(entity, lessonName(entity.getLessonId()));
    }

    private String lessonName(Long lessonId) {
        String lessonName = "Bài học " + lessonId;
        try {
            lessonName = staffClient.getLessonById(lessonId).getName();
        } catch (feign.FeignException e) {
            // An expired subscription does not remove ownership of past results.
            if (e.status() != 403 && e.status() != 404) throw e;
        }
        return lessonName;
    }

    private LessonResultResponse convert(UserExerciseAttempt entity, String lessonName) {
        return LessonResultResponse.builder().resultId(entity.getUserExerciseAttemptId()).lessonId(entity.getLessonId()).lessonName(lessonName).totalQuestion(entity.getTotalQuestion()).correctCount(entity.getCorrectCount()).wrongCount(entity.getWrongCount()).score(entity.getScore()).submittedAt(entity.getSubmittedAt()).build();
    }

    @Override
    public Mono<JapaneseAiResponse> analyzeJapanese(String text) {
        return japaneseAiService.analyzeJapanese(text);
    }

    private CourseDTO mappingCourseToDTO(Course course) {

        return CourseDTO.builder().courseId(course.getCourseId()).courseName(course.getCourseName()).courseDescription(course.getCourseDescription()).levelId(course.getLevelId()).active(course.getActive().getDescription()).packages(course.getPackages().stream().map(this::mappingPackageToDTO).toList()).build();
    }

    private CoursePackageDTO mappingPackageToDTO(CoursePackage p) {
        return CoursePackageDTO.builder().packageId(p.getPackageId()).packageName(p.getPackageName()).durationDays(p.getDurationDays()).price(p.getPrice()).build();
    }

}

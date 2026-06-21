package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.model.dto.CourseDTO;
import com.thang.nihongo_user.model.dto.CoursePackageDTO;
import com.thang.nihongo_user.model.dto.MyCourseDTO;
import com.thang.nihongo_user.repository.ICoursePackageRepository;
import com.thang.nihongo_user.repository.ICourseRepository;
import com.thang.nihongo_user.repository.IUserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final ICourseRepository courseRepository;
    private final ICoursePackageRepository coursePackageRepository;
    private final IUserSubscriptionRepository subscriptionRepository;

    // ================= COURSE =================

    @Override
    @Transactional
    public CourseDTO createNewCourse(Course course) {
        return mappingCourseToDTO(courseRepository.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourse() {
        return courseRepository.findAll()
                .stream()
                .map(this::mappingCourseToDTO)
                .toList();
    }

    // ================= SUBSCRIPTION =================
    @Override
    @Transactional
    public UserSubscription createSubscription(Long userId, Long courseId, Long packageId) {

        if (subscriptionRepository.existsByUserIdAndCourseIdAndPackageId(
                userId, courseId, packageId)) {
            throw new RuntimeException("Already subscribed");
        }

        CoursePackage pack = coursePackageRepository
                .findById(packageId)
                .orElseThrow(() -> new RuntimeException("Package not found"));

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(pack.getDurationDays());

        UserSubscription sub = UserSubscription.builder()
                .userId(userId)
                .courseId(courseId)
                .packageId(packageId)
                .progress(0)
                .createdAt(start)
                .expiredAt(end)
                .build();

        return subscriptionRepository.save(sub);
    }


    @Override
    public List<Long> findCourseIdsByUserId(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .stream()
                .map(UserSubscription::getCourseId)
                .distinct()
                .toList();
    }

    @Override
    public boolean hasActiveSubscription(Long userId, Long courseId) {
        return subscriptionRepository.existsActive(userId, courseId);
    }

    @Override
    public List<MyCourseDTO> findMyCourses(Long userId) {

        return subscriptionRepository.findByUserId(userId)
                .stream()
                .map(sub -> {

                    Course course = courseRepository
                            .findById(sub.getCourseId())
                            .orElseThrow(() -> new RuntimeException("Course not found"));

                    CoursePackage pack = coursePackageRepository
                            .findById(sub.getPackageId())
                            .orElseThrow(() -> new RuntimeException("Package not found"));

                    return MyCourseDTO.builder()
                            .courseId(course.getCourseId())
                            .courseName(course.getCourseName())
                            .packageName(pack.getPackageName())
                            .progress(sub.getProgress())
                            .enrolledAt(sub.getCreatedAt())   // ✅ FIX
                            .expiredAt(sub.getExpiredAt())    // ✅ FIX
                            .build();
                })
                .toList();
    }

    // ================= MAPPING =================

    private CourseDTO mappingCourseToDTO(Course course) {

        return CourseDTO.builder()
                .courseId(course.getCourseId())
                .courseName(course.getCourseName())
                .courseDescription(course.getCourseDescription())
                .levelId(course.getLevelId())
                .active(course.getActive().getDescription())
                .packages(
                        course.getPackages()
                                .stream()
                                .map(this::mappingPackageToDTO)
                                .toList()
                )
                .build();
    }

    private CoursePackageDTO mappingPackageToDTO(CoursePackage p) {
        return CoursePackageDTO.builder()
                .packageId(p.getPackageId())
                .packageName(p.getPackageName())
                .durationDays(p.getDurationDays())
                .price(p.getPrice())
                .build();
    }
}
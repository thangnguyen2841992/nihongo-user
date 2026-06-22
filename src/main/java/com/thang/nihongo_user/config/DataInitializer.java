package com.thang.nihongo_user.config;

import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.CoursePackage;
import com.thang.nihongo_user.model.CourseStatus;
import com.thang.nihongo_user.repository.ICoursePackageRepository;
import com.thang.nihongo_user.repository.ICourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final ICourseRepository courseRepository;
    private final ICoursePackageRepository coursePackageRepository;

    @Bean
    CommandLineRunner initData() {
        return args -> {

            Map<String, Long> courses = Map.of(
                    "Khóa học N5", 5L,
                    "Khóa học N4", 4L,
                    "Khóa học N3", 3L,
                    "Khóa học N2", 2L,
                    "Khóa học N1", 1L
            );

            for (Map.Entry<String, Long> entry : courses.entrySet()) {

                String courseName = entry.getKey();
                Long levelId = entry.getValue();

                if (courseRepository.existsByCourseName(courseName)) {
                    continue;
                }

                Course course = courseRepository.save(
                        Course.builder()
                                .courseName(courseName)
                                .courseDescription(
                                        "Khóa học tiếng Nhật " + courseName
                                )
                                .levelId(levelId)
                                .active(CourseStatus.ACTIVE)
                                .build()
                );

                coursePackageRepository.saveAll(
                        List.of(
                                CoursePackage.builder()
                                        .course(course)
                                        .packageName("BASIC")
                                        .durationDays(30)
                                        .price(BigDecimal.valueOf(299_000))
                                        .isActive(true)
                                        .build(),

                                CoursePackage.builder()
                                        .course(course)
                                        .packageName("STANDARD")
                                        .durationDays(90)
                                        .price(BigDecimal.valueOf(699_000))
                                        .isActive(true)
                                        .build(),

                                CoursePackage.builder()
                                        .course(course)
                                        .packageName("VIP")
                                        .durationDays(365)
                                        .price(BigDecimal.valueOf(1_499_000))
                                        .isActive(true)
                                        .build()
                        )
                );
            }
        };
    }
}
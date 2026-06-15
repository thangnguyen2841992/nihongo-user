package com.thang.nihongo_user.config;

import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.repository.ICourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final ICourseRepository courseRepository;

    @Bean
    CommandLineRunner initData() {
        return args -> {

            List<String> courses = List.of(
                    "Khóa học N5",
                    "Khóa học N4",
                    "Khóa học N3",
                    "Khóa học N2",
                    "Khóa học N1"
            );

            BigDecimal price = BigDecimal.valueOf(1_000_000);

            for (String courseName : courses) {

                if (!courseRepository.existsByCourseName(courseName)) {

                    courseRepository.save(
                            Course.builder()
                                    .courseName(courseName)
                                    .courseDescription("Khóa học tiếng Nhật " + courseName)
                                    .originalPrice(price)
                                    .salePrice(price)
                                    .active(true)
                                    .build()
                    );
                }

                price = price.add(BigDecimal.valueOf(500_000));
            }
        };
    }
}

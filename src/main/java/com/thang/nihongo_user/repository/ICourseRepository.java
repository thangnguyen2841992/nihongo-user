package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICourseRepository extends JpaRepository<Course, Long> {
    boolean existsByCourseName(String courseName);
}

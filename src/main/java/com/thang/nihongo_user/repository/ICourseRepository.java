package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICourseRepository extends JpaRepository<Course, Long> {
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "packages")
    java.util.List<Course> findAll();

    boolean existsByCourseName(String courseName);
}

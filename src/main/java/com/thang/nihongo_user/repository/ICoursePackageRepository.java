package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.CoursePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ICoursePackageRepository extends JpaRepository<CoursePackage, Long> {
    List<CoursePackage> findByCourse_CourseId(Long courseId);
    List<CoursePackage> findByCourse_CourseIdAndIsActiveTrue(Long courseId);
}


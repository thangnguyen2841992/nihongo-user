package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.CoursePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ICoursePackageRepository extends JpaRepository<CoursePackage, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from CoursePackage p join fetch p.course where p.packageId = :id")
    java.util.Optional<CoursePackage> lockById(Long id);
    List<CoursePackage> findByCourse_CourseId(Long courseId);
    List<CoursePackage> findByCourse_CourseIdAndIsActiveTrue(Long courseId);
}


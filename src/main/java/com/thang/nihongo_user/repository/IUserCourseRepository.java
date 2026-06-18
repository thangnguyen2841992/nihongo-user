package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IUserCourseRepository extends JpaRepository<UserCourse, Long> {
    boolean existsByCourseIdAndUserId(Long courseId, Long userId);
}

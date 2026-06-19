package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUserCourseRepository extends JpaRepository<UserCourse, Long> {
    boolean existsByCourseIdAndUserId(Long courseId, Long userId);

    Optional<UserCourse> findByCourseIdAndUserId(Long courseId, Long userId);

    @Query("""
                select uc.courseId
                from UserCourse uc
                where uc.userId = :userId
            """)
    List<Long> findCourseIdsByUserId(
            @Param("userId") Long userId
    );
}

package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IUserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    boolean existsByUserIdAndCourseIdAndPackageId(
            String userId,
            Long courseId,
            Long packageId
    );

    List<UserSubscription> findByUserId(String userId);

    @Query("""
    select count(s) > 0
    from UserSubscription s, Course c
    where c.courseId = s.courseId
    and c.active = com.thang.nihongo_user.model.CourseStatus.ACTIVE
    and s.userId = :userId
    and s.courseId = :courseId
    and s.status = com.thang.nihongo_user.model.SubscriptionStatus.ACTIVE
    and s.expiredAt > current_timestamp
""")
    boolean existsActive(String userId, Long courseId);

    @Query("select count(s) > 0 from UserSubscription s, Course c where c.courseId = s.courseId and c.levelId = :levelId and c.active = com.thang.nihongo_user.model.CourseStatus.ACTIVE and s.userId = :userId and s.status = com.thang.nihongo_user.model.SubscriptionStatus.ACTIVE and s.expiredAt > current_timestamp")
    boolean hasLevelAccess(@org.springframework.data.repository.query.Param("userId") String userId, @org.springframework.data.repository.query.Param("levelId") Long levelId);

    Optional<UserSubscription> findByUserIdAndCourseId(
            String userId,
            Long courseId
    );
}
package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IUserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    boolean existsByUserIdAndCourseIdAndPackageId(
            Long userId,
            Long courseId,
            Long packageId
    );

    List<UserSubscription> findByUserId(Long userId);

    @Query("""
    select count(s) > 0
    from UserSubscription s
    where s.userId = :userId
    and s.courseId = :courseId
    and s.status = com.thang.nihongo_user.model.SubscriptionStatus.ACTIVE
    and s.expiredAt > current_timestamp
""")
    boolean existsActive(Long userId, Long courseId);
}
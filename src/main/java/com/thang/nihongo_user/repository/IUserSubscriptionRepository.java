package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IUserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    @Query("select distinct c.levelId from UserSubscription s,Course c where c.courseId=s.courseId and c.active=com.thang.nihongo_user.model.CourseStatus.ACTIVE and s.userId=:userId and s.status=com.thang.nihongo_user.model.SubscriptionStatus.ACTIVE and s.expiredAt>current_timestamp and c.levelId in :levels")
    List<Long> accessibleLevels(String userId,List<Long> levels);
    @Query("select s.userId from UserSubscription s,Course c where c.courseId=s.courseId and c.active=com.thang.nihongo_user.model.CourseStatus.ACTIVE and s.courseId=:courseId and s.userId in :userIds and s.status=com.thang.nihongo_user.model.SubscriptionStatus.ACTIVE and s.expiredAt>current_timestamp")
    List<String> activeMembers(Long courseId,List<String> userIds);
    org.springframework.data.domain.Page<UserSubscription> findByUserIdAndStatusAndExpiredAtGreaterThanOrderByLastStudiedAtDesc(String userId,com.thang.nihongo_user.model.SubscriptionStatus status,java.time.LocalDateTime now,org.springframework.data.domain.Pageable page);
    interface InactiveLearner { String getUserId(); java.time.LocalDateTime getLastStudiedAt(); long getCourses(); }
    @Query("select s.userId as userId,max(s.lastStudiedAt) as lastStudiedAt,count(s) as courses from UserSubscription s where s.status = com.thang.nihongo_user.model.SubscriptionStatus.ACTIVE and s.expiredAt > :now and not exists (select b from UserSubscription b where b.userId = s.userId and b.lastStudiedAt >= :cutoff) and not exists (select a from UserExerciseAttempt a where a.userId = s.userId and a.submittedAt >= :cutoff) and not exists (select r from StudyActivity r where r.userId = s.userId and r.activityDate >= :cutoffDate) and s.createdAt < :cutoff group by s.userId order by s.userId")
    org.springframework.data.domain.Page<InactiveLearner> inactive(java.time.LocalDateTime now,java.time.LocalDateTime cutoff,java.time.LocalDate cutoffDate,org.springframework.data.domain.Pageable page);

    boolean existsByUserIdAndCourseIdAndPackageId(
            String userId,
            Long courseId,
            Long packageId
    );

    List<UserSubscription> findByUserId(String userId);

    interface ExpiringSubscription { Long getCourseId(); java.time.LocalDateTime getExpiredAt(); }
    @Query("select s.courseId as courseId, s.expiredAt as expiredAt from UserSubscription s where s.userId = :userId and s.status = com.thang.nihongo_user.model.SubscriptionStatus.ACTIVE and s.expiredAt > :now and s.expiredAt <= :until order by s.expiredAt, s.courseId")
    List<ExpiringSubscription> expiring(String userId, java.time.LocalDateTime now, java.time.LocalDateTime until);

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

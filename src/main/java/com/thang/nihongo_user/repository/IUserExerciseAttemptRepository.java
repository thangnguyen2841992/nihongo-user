package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.UserExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IUserExerciseAttemptRepository extends JpaRepository<UserExerciseAttempt, Long> {
    boolean existsByUserId(String userId);
    interface LessonQuality { Long getLessonId(); long getAttempts(); long getLearners(); Double getAverageScore(); }
    interface DecliningLearner { String getUserId(); Double getRecentScore(); Double getPreviousScore(); java.time.LocalDateTime getLastAttempt(); }
    @org.springframework.data.jpa.repository.Query(value = "select a.lessonId as lessonId,count(a) as attempts,count(distinct a.userId) as learners,avg(a.score) as averageScore from UserExerciseAttempt a where a.submittedAt >= :since group by a.lessonId order by avg(a.score) asc,a.lessonId asc",
            countQuery = "select count(distinct a.lessonId) from UserExerciseAttempt a where a.submittedAt >= :since")
    org.springframework.data.domain.Page<LessonQuality> quality(java.time.LocalDateTime since, org.springframework.data.domain.Pageable page);
    @org.springframework.data.jpa.repository.Query("select a.userId as userId,avg(case when a.submittedAt >= :recent then a.score else null end) as recentScore,avg(case when a.submittedAt < :recent then a.score else null end) as previousScore,max(a.submittedAt) as lastAttempt from UserExerciseAttempt a where a.submittedAt >= :baseline group by a.userId having avg(case when a.submittedAt < :recent then a.score else null end) - avg(case when a.submittedAt >= :recent then a.score else null end) >= 10 order by a.userId")
    org.springframework.data.domain.Page<DecliningLearner> declining(java.time.LocalDateTime baseline, java.time.LocalDateTime recent, org.springframework.data.domain.Pageable page);
    interface Summary { long getAttempts(); long getLessons(); Double getAverageScore(); }
    interface DayScore { java.time.LocalDate getDay(); long getAttempts(); Double getAverageScore(); }
    interface LatestLesson { Long getLessonId(); Double getScore(); java.time.LocalDateTime getSubmittedAt(); }
    @org.springframework.data.jpa.repository.Query("select count(a) as attempts, count(distinct a.lessonId) as lessons, avg(a.score) as averageScore from UserExerciseAttempt a where a.userId = :userId")
    Summary summary(String userId);
    @org.springframework.data.jpa.repository.Query("select count(a) from UserExerciseAttempt a where a.userId = :userId and a.score < 80 and not exists (select b from UserExerciseAttempt b where b.userId = a.userId and b.lessonId = a.lessonId and (b.submittedAt > a.submittedAt or (b.submittedAt = a.submittedAt and b.userExerciseAttemptId > a.userExerciseAttemptId)))")
    long countWeakLessons(String userId);
    @org.springframework.data.jpa.repository.Query("select a.lessonId as lessonId, a.score as score, a.submittedAt as submittedAt from UserExerciseAttempt a where a.userId = :userId and a.score < 80 and not exists (select b from UserExerciseAttempt b where b.userId = a.userId and b.lessonId = a.lessonId and (b.submittedAt > a.submittedAt or (b.submittedAt = a.submittedAt and b.userExerciseAttemptId > a.userExerciseAttemptId))) order by a.score asc, a.lessonId asc")
    org.springframework.data.domain.Page<LatestLesson> weakLessons(String userId, org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Query("select cast(a.submittedAt as LocalDate) as day, count(a) as attempts, avg(a.score) as averageScore from UserExerciseAttempt a where a.userId = :userId and a.submittedAt >= :since group by cast(a.submittedAt as LocalDate) order by cast(a.submittedAt as LocalDate)")
    List<DayScore> trend(String userId, java.time.LocalDateTime since);
    @org.springframework.data.jpa.repository.Query("select cast(a.submittedAt as LocalDate) as day, count(a) as attempts, avg(a.score) as averageScore from UserExerciseAttempt a where a.userId = :userId and a.submittedAt >= :since and a.submittedAt < :until group by cast(a.submittedAt as LocalDate) order by cast(a.submittedAt as LocalDate)")
    List<DayScore> trendBetween(String userId, java.time.LocalDateTime since, java.time.LocalDateTime until);
    @org.springframework.data.jpa.repository.Query("select distinct cast(a.submittedAt as LocalDate) from UserExerciseAttempt a where a.userId = :userId and a.submittedAt >= :since order by cast(a.submittedAt as LocalDate) desc")
    List<java.time.LocalDate> activeDates(String userId, java.time.LocalDateTime since);
    long countByUserIdAndSubmittedAtGreaterThanEqualAndSubmittedAtLessThan(String userId, java.time.LocalDateTime from, java.time.LocalDateTime to);
    org.springframework.data.domain.Page<UserExerciseAttempt> findByUserIdAndLessonIdOrderBySubmittedAtDescUserExerciseAttemptIdDesc(String userId, Long lessonId, org.springframework.data.domain.Pageable pageable);
    java.util.Optional<UserExerciseAttempt> findByUserIdAndSubmissionId(String userId, String submissionId);

}

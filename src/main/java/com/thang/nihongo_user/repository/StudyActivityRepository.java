package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.StudyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
public interface StudyActivityRepository extends JpaRepository<StudyActivity, Long> {
    boolean existsByUserIdAndActivityDateAndSourceKey(String userId, LocalDate date, String key);
    long countByUserIdAndActivityDateAndSourceKeyStartingWith(String userId, LocalDate date, String prefix);
    interface DailyCounts { long getCards(); long getPractice(); }
    @Query("select coalesce(sum(case when a.sourceKey like 'card:%' then 1 else 0 end), 0) as cards, coalesce(sum(case when a.sourceKey like 'wrong:%' then 1 else 0 end), 0) as practice from StudyActivity a where a.userId = :userId and a.activityDate = :date")
    DailyCounts dailyCounts(String userId, LocalDate date);
    @Query("select distinct a.activityDate from StudyActivity a where a.userId = :userId order by a.activityDate desc")
    List<LocalDate> activeDates(String userId);
}

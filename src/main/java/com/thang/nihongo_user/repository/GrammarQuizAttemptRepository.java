package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.GrammarQuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface GrammarQuizAttemptRepository extends JpaRepository<GrammarQuizAttempt, Long> {
    interface UnitScore { int getUnit(); long getAttempts(); int getBestPercent(); LocalDateTime getLastSubmittedAt(); }

    @Query("select a.unit as unit, count(a) as attempts, max(a.percent) as bestPercent, max(a.submittedAt) as lastSubmittedAt"
            + " from GrammarQuizAttempt a where a.userId = :userId and a.courseId = :courseId and a.bankId = :bankId"
            + " group by a.unit order by a.unit")
    List<UnitScore> scores(String userId, Long courseId, String bankId);

    @Query("select coalesce(max(a.percent), -1) from GrammarQuizAttempt a"
            + " where a.userId = :userId and a.courseId = :courseId and a.bankId = :bankId and a.unit = :unit")
    int bestPercent(String userId, Long courseId, String bankId, int unit);
}

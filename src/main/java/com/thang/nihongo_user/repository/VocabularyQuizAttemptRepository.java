package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.VocabularyQuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface VocabularyQuizAttemptRepository extends JpaRepository<VocabularyQuizAttempt, Long> {
    interface UnitScore { int getUnit(); long getAttempts(); int getBestPercent(); LocalDateTime getLastSubmittedAt(); }

    @Query("select a.unit as unit, count(a) as attempts, max(a.percent) as bestPercent, max(a.submittedAt) as lastSubmittedAt"
            + " from VocabularyQuizAttempt a where a.userId = :userId and a.courseId = :courseId and a.deckId = :deckId"
            + " group by a.unit order by a.unit")
    List<UnitScore> scores(String userId, Long courseId, String deckId);

    @Query("select coalesce(max(a.percent), -1) from VocabularyQuizAttempt a"
            + " where a.userId = :userId and a.courseId = :courseId and a.deckId = :deckId and a.unit = :unit")
    int bestPercent(String userId, Long courseId, String deckId, int unit);
}

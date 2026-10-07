package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.StudyCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudyCardRepository extends JpaRepository<StudyCard, Long> {
    List<StudyCard> findByUserIdOrderByCreatedAtDesc(String userId);
    List<StudyCard> findByUserIdAndLessonIdOrderByCreatedAtDesc(String userId, Long lessonId);
    List<StudyCard> findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(String userId, LocalDateTime now);
    long countByUserIdAndDueAtLessThanEqual(String userId, LocalDateTime now);
    Optional<StudyCard> findByIdAndUserId(Long id, String userId);
    Optional<StudyCard> findByUserIdAndSourceKey(String userId, String sourceKey);
}

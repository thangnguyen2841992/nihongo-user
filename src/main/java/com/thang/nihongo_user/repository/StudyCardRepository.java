package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.StudyCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudyCardRepository extends JpaRepository<StudyCard, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from StudyCard c where c.id = :id and c.userId = :userId")
    Optional<StudyCard> lockOwned(Long id, String userId);
    org.springframework.data.domain.Page<StudyCard> findByUserIdAndKindOrderByCreatedAtDescIdDesc(String userId, String kind, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<StudyCard> findByUserIdOrderByCreatedAtDescIdDesc(String userId, org.springframework.data.domain.Pageable pageable);
    List<StudyCard> findByUserIdAndDueAtLessThanEqualOrderByDueAtAscIdAsc(String userId, LocalDateTime now, org.springframework.data.domain.Pageable pageable);
    List<StudyCard> findByUserIdOrderByCreatedAtDesc(String userId);
    List<StudyCard> findByUserIdAndLessonIdOrderByCreatedAtDesc(String userId, Long lessonId);
    List<StudyCard> findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(String userId, LocalDateTime now);
    long countByUserIdAndDueAtLessThanEqual(String userId, LocalDateTime now);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<StudyCard> findByIdAndUserId(Long id, String userId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<StudyCard> findByUserIdAndSourceKey(String userId, String sourceKey);
}

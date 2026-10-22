package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.WrongAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WrongAnswerRepository extends JpaRepository<WrongAnswer, Long> {
    interface QuestionQuality { Long getExerciseId(); Long getLessonId(); long getLearners(); java.time.LocalDateTime getLastWrong(); }
    @org.springframework.data.jpa.repository.Query("select w.exerciseId as exerciseId,w.lessonId as lessonId,count(w) as learners,max(w.updatedAt) as lastWrong from WrongAnswer w where w.updatedAt >= :since group by w.exerciseId,w.lessonId order by count(w) desc,w.exerciseId asc")
    org.springframework.data.domain.Page<QuestionQuality> quality(java.time.LocalDateTime since, org.springframework.data.domain.Pageable page);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select w from WrongAnswer w where w.id = :id and w.userId = :userId")
    java.util.Optional<WrongAnswer> lockOwned(Long id, String userId);
    org.springframework.data.domain.Page<WrongAnswer> findByUserIdOrderByUpdatedAtDescIdDesc(String userId, org.springframework.data.domain.Pageable pageable);
    List<WrongAnswer> findByUserIdOrderByUpdatedAtDesc(String userId);
    List<WrongAnswer> findByUserIdAndLessonId(String userId, Long lessonId);
}

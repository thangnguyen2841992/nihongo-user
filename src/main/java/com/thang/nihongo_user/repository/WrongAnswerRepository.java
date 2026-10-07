package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.WrongAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WrongAnswerRepository extends JpaRepository<WrongAnswer, Long> {
    List<WrongAnswer> findByUserIdOrderByUpdatedAtDesc(String userId);
    List<WrongAnswer> findByUserIdAndLessonId(String userId, Long lessonId);
}

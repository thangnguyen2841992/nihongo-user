package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.UserExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IUserExerciseAttemptRepository extends JpaRepository<UserExerciseAttempt, Long> {
        List<UserExerciseAttempt> findByUserUserIdOrderBySubmittedAtDesc(Long userId);

    List<UserExerciseAttempt> findByUserUserIdAndLessonLessonIdOrderBySubmittedAtDesc(
            Long userId,
            Long lessonId
    );

}

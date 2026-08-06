package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.UserExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IUserExerciseAttemptRepository extends JpaRepository<UserExerciseAttempt, Long> {
        List<UserExerciseAttempt> findByUserIdOrderBySubmittedAtDesc(Long userId);

    List<UserExerciseAttempt> findByUserIdAndLessonIdOrderBySubmittedAtDesc(
            Long userId,
            Long lessonId
    );

}

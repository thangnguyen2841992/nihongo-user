package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.UserExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IUserExerciseAttemptRepository extends JpaRepository<UserExerciseAttempt, Long> {
    java.util.Optional<UserExerciseAttempt> findByUserIdAndSubmissionId(String userId, String submissionId);
        List<UserExerciseAttempt> findByUserIdOrderBySubmittedAtDesc(String userId);

    List<UserExerciseAttempt> findByUserIdAndLessonIdOrderBySubmittedAtDesc(
            String userId,
            Long lessonId
    );

}

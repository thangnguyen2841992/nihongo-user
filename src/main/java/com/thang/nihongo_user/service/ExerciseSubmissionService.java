package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.dto.*;
import com.thang.nihongo_user.repository.IUserExerciseAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExerciseSubmissionService {
    private final ExerciseAttemptService worker;
    private final IUserExerciseAttemptRepository attempts;

    public ExerciseGrade submit(String userId, SubmitLessonResultRequest request) {
        try { return worker.submit(userId, request); }
        catch (DataIntegrityViolationException collision) {
            // The worker transaction has rolled back before reading the winning request.
            if (request.getSubmissionId() == null) throw collision;
            var existing = attempts.findByUserIdAndSubmissionId(userId, request.getSubmissionId());
            if (existing.isEmpty()) throw collision;
            return ExerciseAttemptService.replay(existing.get(), request);
        }
    }
}

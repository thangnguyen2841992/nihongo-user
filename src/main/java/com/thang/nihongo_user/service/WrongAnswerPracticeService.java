package com.thang.nihongo_user.service;

import com.thang.nihongo_user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Map;

@Service @RequiredArgsConstructor
public class WrongAnswerPracticeService {
    private final WrongAnswerRepository wrongAnswers;
    private final IStaffClient staff;
    private final StudyActivityService activity;
    public record Result(boolean correct, String correctAnswer) {}

    @Transactional
    public Result practice(String userId, Long id, String answer) {
        if (answer == null || !answer.matches("[A-D]"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn đáp án A, B, C hoặc D");
        var wrong = wrongAnswers.lockOwned(id, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy câu cần luyện"));
        // Recheck the current authoritative key and entitlement, without recording a partial lesson attempt.
        var grade = staff.grade(wrong.getLessonId(), Map.of(wrong.getExerciseId(), answer));
        String correct = grade == null || grade.correctAnswers() == null ? null : grade.correctAnswers().get(wrong.getExerciseId());
        if (correct == null || !correct.matches("[A-D]"))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Câu hỏi đã thay đổi. Hãy tải lại bài học");
        var now = LocalDateTime.now();
        activity.record(userId, "wrong:" + wrong.getExerciseId(), now.toLocalDate());
        if (correct.equals(answer)) wrongAnswers.delete(wrong);
        else { wrong.setChosenAnswer(answer); wrong.setCorrectAnswer(correct); wrong.setUpdatedAt(now); wrongAnswers.save(wrong); }
        return new Result(correct.equals(answer), correct);
    }

    @Transactional
    public void master(String userId, Long id) {
        wrongAnswers.delete(wrongAnswers.lockOwned(id, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy câu cần luyện")));
    }
}

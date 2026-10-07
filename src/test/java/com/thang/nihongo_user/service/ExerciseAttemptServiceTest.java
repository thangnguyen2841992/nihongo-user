package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.WrongAnswer;
import com.thang.nihongo_user.model.dto.ExerciseGrade;
import com.thang.nihongo_user.model.dto.SubmitLessonResultRequest;
import com.thang.nihongo_user.repository.IStaffClient;
import com.thang.nihongo_user.repository.IUserExerciseAttemptRepository;
import com.thang.nihongo_user.repository.WrongAnswerRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExerciseAttemptServiceTest {
    private final IStaffClient staff = mock(IStaffClient.class);
    private final IUserExerciseAttemptRepository attempts = mock(IUserExerciseAttemptRepository.class);
    private final WrongAnswerRepository wrong = mock(WrongAnswerRepository.class);
    private final ExerciseAttemptService service = new ExerciseAttemptService(staff, attempts, wrong);

    @Test void storesWrongAnswerAndRemovesItAfterCorrection() {
        SubmitLessonResultRequest request = new SubmitLessonResultRequest();
        request.setLessonId(9L); request.setAnswers(Map.of(2L, "B"));
        when(staff.grade(eq(9L), any())).thenReturn(new ExerciseGrade(1, 0, 1, Map.of(2L, "A"), Map.of()));
        service.submit("owner", request);
        var captured = org.mockito.ArgumentCaptor.forClass(WrongAnswer.class);
        verify(wrong).save(captured.capture());
        assertEquals("owner", captured.getValue().getUserId());
        assertEquals("B", captured.getValue().getChosenAnswer());

        request.setAnswers(Map.of(2L, "A"));
        when(staff.grade(eq(9L), any())).thenReturn(new ExerciseGrade(1, 1, 0, Map.of(2L, "A"), Map.of()));
        when(wrong.findByUserIdAndLessonId("owner", 9L)).thenReturn(List.of(captured.getValue()));
        service.submit("owner", request);
        verify(wrong).delete(captured.getValue());
    }
}

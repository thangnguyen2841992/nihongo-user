package com.thang.nihongo_user.service;
import com.thang.nihongo_user.model.UserExerciseAttempt;
import com.thang.nihongo_user.model.dto.*;
import com.thang.nihongo_user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
@Service @RequiredArgsConstructor
public class ExerciseAttemptService {
 private final IStaffClient staff;
 private final IUserExerciseAttemptRepository attempts;
 public ExerciseGrade submit(String userId,SubmitLessonResultRequest request) {
  // Staff validates entitlement and grades authoritative answers using the forwarded JWT.
  ExerciseGrade grade=staff.grade(request.getLessonId(),request.getAnswers());
  if(grade==null||grade.totalQuestion()<=0||grade.correctCount()<0||grade.correctCount()>grade.totalQuestion())
   throw new IllegalStateException("Invalid grading response");
  attempts.save(UserExerciseAttempt.builder().userId(userId).lessonId(request.getLessonId())
   .totalQuestion(grade.totalQuestion()).correctCount(grade.correctCount()).wrongCount(grade.wrongCount())
   .score(100.0*grade.correctCount()/grade.totalQuestion()).submittedAt(LocalDateTime.now()).build());
  return grade;
 }
}

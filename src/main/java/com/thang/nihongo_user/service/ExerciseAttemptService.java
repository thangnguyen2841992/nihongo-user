package com.thang.nihongo_user.service;
import com.thang.nihongo_user.model.UserExerciseAttempt;
import com.thang.nihongo_user.model.dto.*;
import com.thang.nihongo_user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import com.thang.nihongo_user.model.WrongAnswer;
@Service @RequiredArgsConstructor
public class ExerciseAttemptService {
 private final IStaffClient staff;
 private final IUserExerciseAttemptRepository attempts;
 private final WrongAnswerRepository wrongAnswers;
 @Transactional
 public ExerciseGrade submit(String userId,SubmitLessonResultRequest request) {
  // Staff validates entitlement and grades authoritative answers using the forwarded JWT.
  ExerciseGrade grade=staff.grade(request.getLessonId(),request.getAnswers());
  if(grade==null||grade.totalQuestion()<=0||grade.correctCount()<0||grade.correctCount()>grade.totalQuestion()
          ||grade.wrongCount()!=grade.totalQuestion()-grade.correctCount()
          ||grade.correctAnswers()==null||grade.correctAnswers().size()!=grade.totalQuestion())
   throw new IllegalStateException("Invalid grading response");
  attempts.save(UserExerciseAttempt.builder().userId(userId).lessonId(request.getLessonId())
   .totalQuestion(grade.totalQuestion()).correctCount(grade.correctCount()).wrongCount(grade.wrongCount())
   .score(100.0*grade.correctCount()/grade.totalQuestion()).submittedAt(LocalDateTime.now()).build());
  // Only the grading service's answer key may determine which answers are wrong.
  Map<Long, WrongAnswer> previous = wrongAnswers.findByUserIdAndLessonId(userId, request.getLessonId())
          .stream().collect(Collectors.toMap(WrongAnswer::getExerciseId, answer -> answer));
  for (var answer : grade.correctAnswers().entrySet()) {
   String chosen = request.getAnswers().get(answer.getKey());
   if (answer.getValue().equalsIgnoreCase(chosen == null ? "" : chosen)) {
    WrongAnswer old = previous.remove(answer.getKey());
    if (old != null) wrongAnswers.delete(old);
   } else {
    WrongAnswer wrong = previous.remove(answer.getKey());
    if (wrong == null) {
     wrong = new WrongAnswer(); wrong.setUserId(userId); wrong.setLessonId(request.getLessonId());
     wrong.setExerciseId(answer.getKey());
    }
    wrong.setChosenAnswer(chosen); wrong.setCorrectAnswer(answer.getValue()); wrong.setUpdatedAt(LocalDateTime.now());
    wrongAnswers.save(wrong);
   }
  }
  wrongAnswers.deleteAll(previous.values());
  return grade;
 }
}

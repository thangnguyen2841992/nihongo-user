package com.thang.nihongo_user.model.dto;
import java.util.Map;
public record ExerciseGrade(int totalQuestion,int correctCount,int wrongCount,Map<Long,String> correctAnswers,
                            Map<Long,Map<String,Object>> aiSolutions, int unansweredCount) {
 public ExerciseGrade(int totalQuestion, int correctCount, int wrongCount,
                      Map<Long,String> correctAnswers, Map<Long,Map<String,Object>> aiSolutions) {
  this(totalQuestion, correctCount, wrongCount, correctAnswers, aiSolutions, 0);
 }
}

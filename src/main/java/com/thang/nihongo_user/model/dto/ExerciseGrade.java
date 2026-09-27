package com.thang.nihongo_user.model.dto;
import java.util.Map;
public record ExerciseGrade(int totalQuestion,int correctCount,int wrongCount,Map<Long,String> correctAnswers) {}

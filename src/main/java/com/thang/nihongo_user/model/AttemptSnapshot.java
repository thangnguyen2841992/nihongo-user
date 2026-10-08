package com.thang.nihongo_user.model;

import com.thang.nihongo_user.model.dto.ExerciseGrade;
import java.util.Map;

public record AttemptSnapshot(Map<Long, String> chosenAnswers, ExerciseGrade grade) {}

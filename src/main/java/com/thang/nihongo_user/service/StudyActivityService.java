package com.thang.nihongo_user.service;
import com.thang.nihongo_user.model.StudyActivity;
import com.thang.nihongo_user.repository.StudyActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service @RequiredArgsConstructor
public class StudyActivityService {
    private final StudyActivityRepository activities;
    // Call only while holding the source card/wrong-answer row lock in the enclosing transaction.
    public void record(String userId, String key, LocalDate date) {
        if (!activities.existsByUserIdAndActivityDateAndSourceKey(userId, date, key)) {
            var activity = new StudyActivity();
            activity.setUserId(userId); activity.setSourceKey(key); activity.setActivityDate(date);
            activities.save(activity);
        }
    }
}
